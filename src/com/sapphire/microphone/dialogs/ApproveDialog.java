package com.sapphire.microphone.dialogs;


import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import com.sapphire.microphone.R;

public class ApproveDialog extends Dialog implements View.OnClickListener {
    private View.OnClickListener listener;
    private String text = null;
    private TextView message;

    public ApproveDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.approve_dialog);
        message = (TextView) findViewById(R.id.message);
        if (text != null)
            message.setText(text);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
    }

    public void setMessage(final String text) {
        if (message != null) {
            message.setText(text);
        }
        this.text = text;
    }

    public void setOnClickListener(final View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        dismiss();
        listener.onClick(v);
    }
}
