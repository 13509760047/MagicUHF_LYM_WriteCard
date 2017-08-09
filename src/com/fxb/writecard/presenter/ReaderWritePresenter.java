package com.fxb.writecard.presenter;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fxb.writecard.model.AbstractUHFModel;
import com.fxb.writecard.model.IResponse;
import com.fxb.writecard.model.ReadModel;
import com.fxb.writecard.util.DevBeep;
import com.fxb.writecard.view.IUHFView;
import com.olc.uhf.tech.ISO1800_6C;

import java.io.UnsupportedEncodingException;

/**
 * Created by zf on 2017/8/4 0004.
 */

public class ReaderWritePresenter {

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private final String TAG = ReaderWritePresenter.class.getSimpleName();

    private IUHFView iuhfView;

    private ISO1800_6C uhf_6c;

    private AbstractUHFModel abstractUHFModel;

    private ReaderWritePresenter() {
    }

    public ReaderWritePresenter(IUHFView iuhfView) {
        this.iuhfView = iuhfView;
        this.abstractUHFModel = new ReadModel();
    }

    public void setEPCtext() {
        String epc = abstractUHFModel.inventory();
        if (epc == null) {
            return;
        }
        iuhfView.setEPCtext(epc);
    }

    public void readCard() {
        final int sa = 0;
        final int dl = 32;
        new Thread(new Runnable() {
            @Override
            public void run() {
                read(sa, dl);
                DevBeep.PlayOK();
            }
        }).start();
    }

    private void read(final int sa, final int dl) {
        abstractUHFModel.setnSA(sa);
        abstractUHFModel.setnDL(dl);
        abstractUHFModel.ReadDate(new IResponse() {
            @Override
            public void Response(byte[] bytes) {
                String str = null;
                try {
                    str = new String(bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void Error(int code, String error) {
                if (code != 0) {
                    read(sa, dl);
                }
            }
        });
    }

    public void writeCard(final String sb) {
        if (sb == null) {
            Log.e(TAG, "sb is null");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String str = sb;
                    while (str.length() < 32) {
                        str += "00000000000000000000";
                    }
                    byte[] date = str.getBytes("UTF-8");
                    int nSA = 0;
                    final int nDL = 10;
                    int position = 0;
                    int word = (32 - nSA) * 2;
                    int datelength = date.length;
                    //限制写入字节数 总可写入字节数-已写入字节数
                    int date2length = word < datelength ? word : datelength;
                    byte[] date2 = new byte[date2length];
                    int size = date2length / nDL * 2 + 1;
                    System.arraycopy(date, 0,
                            date2, 0, date2length);
                    byte[] pwrite;
                    while (true) {
                        if (size == position) {
                            break;
                        }
                        int length = (nDL * 2) * position;
                        /**
                         * 每次只能写入20byte数据 所以分层写入 最大读写20byte 不足20byte 写剩余的部分
                         */
                        int weitelength = date2length - length > nDL * 2 ? nDL * 2 : date2length - length;
                        pwrite = new byte[weitelength];
                        System.arraycopy(date2, length, pwrite, 0, weitelength);
                        //设置写入长度 剩余可写入长度不足时会报176 一次写入最大长度为10word
                        int mnDL = weitelength > nDL * 2 ? nDL : weitelength % 2 == 0 ? weitelength / 2 : weitelength / 2 + 1;
                        write(pwrite, nSA, mnDL);
                        if (weitelength < nDL * 2) {
                            break;
                        }
                        position++;
                        nSA += 10;
                    }
                    DevBeep.PlayOK();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void write(final byte[] date, final int nSA, final int nDL) {
        abstractUHFModel.setnSA(nSA);
        abstractUHFModel.setnDL(nDL * 2);
        abstractUHFModel.WriteDate(date, new IResponse() {
            @Override
            public void Response(byte[] bytes) {

            }

            @Override
            public void Error(int code, String error) {
                if (code != 0) {
                    write(date, nSA, nDL);
                }
            }
        });
    }
}



