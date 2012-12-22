package com.mathieuclement.android.kjunior.remote.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Author: Mathieu Clément
 * Date: 22.12.2012
 */
public class ChooseDialogFragment extends DialogFragment {
    public static ChooseDialogFragment newInstance(String title, String[] elements) {

        ChooseDialogFragment frag = new ChooseDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putStringArray("elements", elements);
        frag.setArguments(args);
        return frag;
    }

    private ChooseDialogListener dialogListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String[] elements = getArguments().getStringArray("elements");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogListener.onChooseDialogCancelled(ChooseDialogFragment.this);
                    }
                })
                .setItems(elements, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item

                        dialogListener.onChooseDialogItemClicked(ChooseDialogFragment.this, which);
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            dialogListener = (ChooseDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw an exception
            throw new ClassCastException(activity.toString()
                    + " must implement " + ChooseDialogListener.class.getSimpleName());
        }
    }

    public interface ChooseDialogListener {
        public void onChooseDialogItemClicked(DialogFragment dialog, int which);

        public void onChooseDialogCancelled(DialogFragment dialog);
    }
}
