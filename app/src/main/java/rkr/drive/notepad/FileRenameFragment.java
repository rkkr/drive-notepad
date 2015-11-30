package rkr.drive.notepad;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class FileRenameFragment extends DialogFragment {

    public interface EditNameDialogListener {
        void onFinishEditDialog(String inputText);
    }

    public FileRenameFragment(){};

    public static FileRenameFragment newInstance(String currentFileName) {
        FileRenameFragment frag = new FileRenameFragment();
        Bundle args = new Bundle();
        args.putString("fileName", currentFileName);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_file_rename, container);

        final EditText mEditText = (EditText) view.findViewById(R.id.editFileName);
        mEditText.setText(getArguments().getString("fileName", "Untitled document"));
        mEditText.requestFocus();

        getDialog().setTitle("Rename document");
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button cancelButton = (Button) view.findViewById(R.id.button_rename_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });
        Button okButton = (Button) view.findViewById(R.id.button_rename_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditNameDialogListener listener = (EditNameDialogListener) getActivity();
                listener.onFinishEditDialog(mEditText.getText().toString());
                dismiss();
            }
        });
        return view;
    }
}
