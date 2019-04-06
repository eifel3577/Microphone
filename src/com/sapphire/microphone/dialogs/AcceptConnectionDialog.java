package com.sapphire.microphone.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.sapphire.microphone.R;

public class AcceptConnectionDialog extends Dialog implements
		View.OnClickListener {
	private View.OnClickListener listener;
	private String text = null;
	private TextView message;

	public AcceptConnectionDialog(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		setContentView(R.layout.accept_connection_dialog_layout);
		message = (TextView) findViewById(R.id.message);
		if (text != null)
			message.setText(text);
		findViewById(R.id.ok).setOnClickListener(this);
		findViewById(R.id.cancel_button).setOnClickListener(this);
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
