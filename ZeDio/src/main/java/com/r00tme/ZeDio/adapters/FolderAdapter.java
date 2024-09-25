package com.r00tme.ZeDio.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter to handle files and folders with functionality to play, stop, edit, and delete items.
 * It also tracks currently playing media files.
 */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private final List<File> files;
    private final Context context;
    private final OnItemClickListener listener;
    private File currentlyPlayingFile = null;  // Track currently playing file
    private static final Logger logger = LoggerFactory.getLogger(FolderAdapter.class);  // Logger initialization

    public FolderAdapter(List<File> files, Context context, OnItemClickListener listener) {
        this.files = files;
        this.context = context;
        this.listener = listener;
        // Sort files and folders: directories first, then files
        sortFilesAndFolders();
    }

    // Sort the files: directories on top, files below
    private void sortFilesAndFolders() {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && f2.isFile()) {
                    return -1; // Place directories before files
                } else if (f1.isFile() && f2.isDirectory()) {
                    return 1;  // Place files after directories
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());  // Alphabetical order for both directories and files
                }
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);

        if (file.isDirectory()) {
            holder.icon.setImageResource(R.drawable.music_folder);
        } else if (file.isFile() && file.getName().endsWith(".mp3")) {
            if (file.equals(currentlyPlayingFile)) {
                holder.icon.setImageResource(R.drawable.play);  // Set the playing icon
            } else {
                holder.icon.setImageResource(R.drawable.song);  // Set the normal song icon
            }
        }

        holder.name.setText(file.getName());

        // Handle edit button
        holder.editButton.setOnClickListener(v -> {
            showEditDialog(file, holder.name);
        });

        // Handle delete button
        holder.deleteButton.setOnClickListener(v -> {

            showDeleteConfirmationDialog(file, position); // Show confirmation dialog before deleting
        });

        // Handle item clicks to play or stop
        holder.itemView.setOnClickListener(v -> {
            if (file.isFile() && file.getName().endsWith(".mp3")) {
                if (currentlyPlayingFile != null && currentlyPlayingFile.equals(file)) {
                    listener.onStopPlaying(file); // Stop playing the file if already playing
                    currentlyPlayingFile = null;
                } else {
                    currentlyPlayingFile = file;
                    listener.onPlayItem(file);  // Play the file
                }
            } else if (file.isDirectory()) {
                listener.onFolderClick(file);  // Handle folder click
                currentlyPlayingFile = null;
            }
            notifyDataSetChanged();  // Refresh the icons for all items
        });
    }
    /**
     * Shows a confirmation dialog before deleting a file or folder.
     *
     * @param file The file or folder to delete.
     * @param position The position in the list of files.
     */
    private void showDeleteConfirmationDialog(File file, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete " + file.getName() + "?");

        builder.setPositiveButton("Ok", (dialog, which) -> {
            // Perform deletion if user confirms
            if (file.isFile()) {
                if (file.delete()) {
                    files.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                    logger.info("File deleted successfully: {}", file.getName());
                } else {
                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    logger.error("Failed to delete file: {}", file.getName());
                }
            } else if (file.isDirectory()) {
                deleteDirectory(file, position);  // Call deleteDirectory method to handle folder deletion
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled the dialog, no action needed
            dialog.cancel();
            logger.info("Delete operation cancelled for file: {}", file.getName());
        });

        builder.show();
    }
    @Override
    public int getItemCount() {
        return files.size();
    }

    /**
     * Deletes the directory and all its contents.
     *
     * @param directory The directory to delete.
     * @param position The position in the list to remove.
     */
    private void deleteDirectory(File directory, int position) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file, position);
                } else {
                    file.delete();
                }
            }
            if (directory.delete()) {
                files.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show();
                logger.info("Directory deleted successfully: {}", directory.getName());
            } else {
                Toast.makeText(context, "Failed to delete folder", Toast.LENGTH_SHORT).show();
                logger.error("Failed to delete folder: {}", directory.getName());
            }
        }
    }

    /**
     * Shows a dialog to edit the file or folder name.
     *
     * @param file The file or folder to rename.
     * @param fileNameView The TextView displaying the current name.
     */
    private void showEditDialog(File file, TextView fileNameView) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit File/Folder Name");

        final EditText input = new EditText(context);
        input.setText(file.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString();
            File newFile = new File(file.getParent(), newName);
            if (file.renameTo(newFile)) {
                fileNameView.setText(newName);
                Toast.makeText(context, "File/Folder renamed", Toast.LENGTH_SHORT).show();
                logger.info("File/Folder renamed successfully from {} to {}", file.getName(), newName);
            } else {
                Toast.makeText(context, "Failed to rename File/Folder", Toast.LENGTH_SHORT).show();
                logger.error("Failed to rename file: {}", file.getName());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            logger.info("Rename operation cancelled for file: {}", file.getName());
        });

        builder.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;
        ImageView editButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.file_name);
            icon = itemView.findViewById(R.id.file_icon);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }

    /**
     * Listener interface to handle folder clicks and media file play/stop.
     */
    public interface OnItemClickListener {
        void onPlayItem(File file);  // When an item is played
        void onStopPlaying(File file);  // When an item is stopped
        void onFolderClick(File folder);  // Handle folder clicks
    }
}
