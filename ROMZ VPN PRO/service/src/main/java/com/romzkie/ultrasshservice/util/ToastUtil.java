package com.romzkie.ultrasshservice.util;

import android.content.*;
import com.sdsmdg.tastytoast.*;

public class ToastUtil
{

	private Context mContext;
	public ToastUtil(Context c) {
		mContext = c;
	}

	public void showSuccessToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.SUCCESS);
    }

    public void showWarningToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.WARNING);
    }

    public void showErrorToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.ERROR);
    }
    public void showInfoToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.INFO);
    }

    public void showDefaultToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.DEFAULT);
    }


    public void showConfusingToast(String msg) {
        TastyToast.makeText(mContext, msg, TastyToast.LENGTH_LONG,
							TastyToast.CONFUSING);
    }
}
