package com.r00tme.ZeDio.classes;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.r00tme.ZeDio.R;

public class Helper {

    // Method to show a custom toast
    public void Toast(Context context, LayoutInflater inflater, String message, boolean isSuccess, boolean showAtTop) {
        // Inflate the custom layout
        View layout = inflater.inflate(R.layout.toast, null);

        // Set the text and image for the toast
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        ImageView icon = layout.findViewById(R.id.toast_image);
        if (isSuccess) {
            icon.setImageResource(R.drawable.success);
        } else {
            icon.setImageResource(R.drawable.error);  // Error icon
        }

        // Create the Toast
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

        // Position the toast based on the `showAtTop` flag
        if (showAtTop) {
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);  // Offset from the top
        } else {
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);  // Offset from the bottom
        }

        // Show the custom toast
        toast.show();
    }
}
