package buw.sangnguyenminh_118031;

import android.graphics.*;
import android.support.v4.content.*;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.net.*;

import pl.droidsonroids.gif.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String ACTIVE_TAB_COLOR = "@android:color/holo_green_light";
    private final AppCompatActivity ACTIVITY = this;
    private String strLastURL;
    private String strResult;
    private Bitmap objImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Initialise loading image
            GifDrawable objDrawable = new GifDrawable(getResources(), R.drawable.ic_loading);
            GifImageView objImageView = (GifImageView) findViewById(R.id.imgLoading);
            objImageView.setImageDrawable(objDrawable);

            // Initialise tab button
            Button btnPlain = (Button) findViewById(R.id.btnPlain);
            btnPlain.setBackground(ContextCompat.getDrawable(ACTIVITY, R.color.tabButton));
            btnPlain.setTextColor(Color.WHITE);
            Button btnHTML = (Button) findViewById(R.id.btnHTML);
            btnHTML.setBackground(ContextCompat.getDrawable(ACTIVITY, R.color.tabButton));
            btnHTML.setTextColor(Color.WHITE);
            Button btnImage = (Button) findViewById(R.id.btnImage);
            btnImage.setBackground(ContextCompat.getDrawable(ACTIVITY, R.color.tabButton));
            btnImage.setTextColor(Color.WHITE);
        } catch (Exception e) {
            Log.e("onCreate", "Exception", e);
        }
    }

    //region Common

    // Toast wrapper
    private void showToast(final String strMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast objToast = Toast.makeText(ACTIVITY, strMessage, Toast.LENGTH_SHORT);
                objToast.show();
            }
        });
    }

    private void greyOutButton(final View objButton) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                objButton.setAlpha(.5f);
            }
        });
    }

    private void disableButton(final View objButton) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                objButton.setEnabled(false);
                greyOutButton(objButton);
            }
        });
    }

    private void greyInButton(final View objButton) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                objButton.setAlpha(1f);
            }
        });
    }

    private void enableButton(final View objButton) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                objButton.setEnabled(true);
                greyInButton(objButton);
            }
        });
    }

    //endregion

    @Override
    public void onClick(View v) {
        int intID = v.getId();

        if (intID == R.id.btnConnect) {
            clickConnectButton();
        } else {
            setTabActive((Button) v);
            setCurrentResultViewer(intID);

            switch (intID) {
                case R.id.btnPlain:
                    showPlain();
                    break;

                case R.id.btnHTML:
                    showHTML();
                    break;

                default:
                    break;
            }
        }
    }

    private void setCurrentResultViewer(final int intID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imgResult = (ImageView) findViewById(R.id.imgResult);
                TextView tvwResult = (TextView) findViewById(R.id.tvwResult);

                if (intID == R.id.btnImage) {
                    imgResult.setVisibility(View.VISIBLE);
                    tvwResult.setVisibility(View.INVISIBLE);
                    showImage();
                } else {
                    imgResult.setVisibility(View.INVISIBLE);
                    tvwResult.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    //region Connect

    private void clickConnectButton() {
        try {
            final EditText txtURL = (EditText) findViewById(R.id.txtURL);
            String strInput = txtURL.getText().toString();

            if (TextUtils.isEmpty(strInput)) {
                showToast("Please enter URL");
            } else {
                final String strFixed = autoFixURL(strInput);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtURL.setText(strFixed);
                    }
                });

                connect(strFixed);
            }
        } catch (Exception e) {
            Log.e("onClick", "Exception", e);
        } finally {
            finishLoading();
        }
    }

    private String autoFixURL(String strURL) {
        // Auto add htpp://
        if (strURL.indexOf("http://") < 0) {
            strURL = "http://" + strURL;
        }

        int intDotCount = strURL.length() - strURL.replace(".", "").length();

        // If there are not at least 2 dots and does not have www already, auto add www
        if (intDotCount < 2 && strURL.indexOf("http://www.") < 0) {
            strURL = "http://www." + strURL.substring("http://".length());
        }

        return strURL;
    }

    private void connect(String strInput) {
        // Try to create URL object from user input, show error message if input is invalid
        try {
            final URL objURL = new URL(strInput);
            final String strURL = strInput;

            // Network operation cannot be executed on the main thread
            Thread objThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    connectThreadAction(objURL, strURL);
                }
            });

            objThread.start();
        } catch (MalformedURLException e) {
            showToast("Invalid URL");
        }
    }

    private void connectThreadAction(URL objURL, String strURL) {
        startLoading();
        boolean bolCheck = checkURLResponseCode(objURL);

        // Only load content if able to connect
        if (bolCheck) {
            boolean bolResult = loadContentFromURL(objURL, strURL);

            // If succuess, save last URL
            if (bolResult) {
                strLastURL = strURL;
            }
        } else {
            finishLoading();
        }
    }

    private boolean loadContentFromURL(URL objURL, String strURL) {
        // Remove current image, if any;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imgResult = (ImageView) findViewById(R.id.imgResult);
                imgResult.setImageDrawable(null);
            }
        });

        objImage = null;
        strResult = "";
        String strLine = "";

        try {
            InputStream objStream = objURL.openStream();
            InputStreamReader objStreamReader = new InputStreamReader(objStream);
            BufferedReader objReader = new BufferedReader(objStreamReader);

            while (strLine != null) {
                strLine = objReader.readLine();

                // Concatenate if line is not empty
                if (strLine != null) {
                    strResult += strLine;
                }
            }

            objStreamReader.close();
            finishLoadContent(strURL);
            return true;
        } catch (IOException e) {
            showToast("Unable to connect, check if URL is valid");
            return false;
        } finally {
            finishLoading();
        }
    }

    private void finishLoadContent(String strURL) {
        showResultView();
        finishLoading();
        boolean bolIsImage = checkURLIsImage(strURL);

        if (bolIsImage) {
            onClick(findViewById(R.id.btnImage));
        } else {
            onClick(findViewById(R.id.btnPlain));
        }
    }

    private boolean checkURLIsImage(String strURL) {
        String strExtension = "";
        int intIndex = strURL.lastIndexOf('.');

        if (intIndex >= 0) {
            strExtension = strURL.substring(intIndex + 1).toLowerCase();
        }

        if (strExtension.equals("jpg") || strExtension.equals("png")) {
            return true;
        } else {
            return false;
        }
    }

    private void showResultView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Show result panel
                TabHost thtResult = (TabHost) findViewById(R.id.thtResult);
                thtResult.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean checkURLResponseCode(URL objURL) {
        try {
            HttpURLConnection objConnection = (HttpURLConnection) objURL.openConnection();
            objConnection.setRequestMethod("GET");
            objConnection.connect();
            int intCode = objConnection.getResponseCode();
            boolean bolResult = checkCode(intCode);
            return bolResult;
        } catch (SocketTimeoutException e) {
            showToast("Connection timeout");
            return false;
        } catch (ConnectException e) {
            showToast("Connection error: " + e.getMessage());
            return false;
        } catch (IOException e) {
            showToast("Unable to connect, check if URL is valid");
            return false;
        }
    }

    private boolean checkCode(int intCode) {
        switch (intCode) {
            case 400:
                showToast("Error: 400 - Bad request");
                return false;

            case 401:
                showToast("Error: 401 - Unauthorized");
                return false;

            case 403:
                showToast("Error: 403 - Forbidden");
                return false;

            case 404:
                showToast("Error: 404 - Not Found");
                return false;

            case 408:
                showToast("Error: 408 - Request Timeout");
                return false;

            case 500:
                showToast("Error: 500 - Internal Server Error");
                return false;

            case 502:
                showToast("Error: 502 - Bad Gateway");
                return false;

            case 503:
                showToast("Error: 503 - Service Unavailable");
                return false;

            case 504:
                showToast("Error: 504 - Gateway Time-out");
                return false;

            default:
                return true;
        }
    }

    private void startLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Disable connect button
                    Button btnConnect = (Button) findViewById(R.id.btnConnect);
                    disableButton(btnConnect);

                    // Show loading icon
                    ImageView imgLoading = (ImageView) findViewById(R.id.imgLoading);
                    imgLoading.setVisibility(View.VISIBLE);

                    // Hide result panel
                    TabHost thtResult = (TabHost) findViewById(R.id.thtResult);
                    thtResult.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    Log.e("startLoading", "Exception", e);
                }
            }
        });
    }

    private void finishLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Enable connect button
                    Button btnConnect = (Button) findViewById(R.id.btnConnect);
                    enableButton(btnConnect);

                    // Hide loading icon
                    ImageView imgLoading = (ImageView) findViewById(R.id.imgLoading);
                    imgLoading.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    Log.e("finishLoading", "Exception", e);
                }
            }
        });
    }

    //endregion

    //region Tabs

    private void setTabActive(Button objButton) {
        Button btnPlain = (Button) findViewById(R.id.btnPlain);
        greyOutButton(btnPlain);
        Button btnHTML = (Button) findViewById(R.id.btnHTML);
        greyOutButton(btnHTML);
        Button btnImage = (Button) findViewById(R.id.btnImage);
        greyOutButton(btnImage);
        greyInButton(objButton);
    }

    private void showPlain() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tvwPlain = (TextView) findViewById(R.id.tvwResult);
                tvwPlain.setText(strResult);
            }
        });
    }

    private void showHTML() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    TextView tvwPlain = (TextView) findViewById(R.id.tvwResult);
                    tvwPlain.setText(Html.fromHtml(strResult));
                } catch (Exception ex) {
                    showToast("Cannot display HTML, check if content is valid");
                }
            }
        });
    }

    private void showImage() {
        // If image has not been processed
        if (objImage == null) {
            try {
                final URL objURL = new URL(strLastURL);

                Thread objThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getImageFromURL(objURL);
                    }
                });

                objThread.start();
            } catch (Exception ex) {
                showToast("Cannot display image, check if image is valid");
            }
        } else {
            setImageToView();
        }
    }

    private void getImageFromURL(URL objURL) {
        try {
            byte[] arrImageByte = getImageBytes(objURL);

            if (arrImageByte != null) {
                BitmapFactory.Options objBitmapOptions = new BitmapFactory.Options();
                objImage = BitmapFactory.decodeByteArray(arrImageByte, 0, arrImageByte.length, objBitmapOptions);

                if (objBitmapOptions.outWidth != -1 && objBitmapOptions.outHeight != -1) {
                    setImageToView();
                } else {
                    showToast("Not a valid image");
                }
            } else {
                showToast("Cannot display image, check if image is valid");
            }
        } catch (Exception ex) {
            showToast("Cannot display image, check if image is valid");
        }
    }

    private byte[] getImageBytes(URL objURL) {
        try {
            InputStream objInputStream = new BufferedInputStream(objURL.openStream());
            ByteArrayOutputStream objOutputStream = new ByteArrayOutputStream();
            byte[] arrBuffer = new byte[1024];
            int intTemp = 0;

            while (intTemp != -1) {
                intTemp = objInputStream.read(arrBuffer);

                if (intTemp != -1) {
                    objOutputStream.write(arrBuffer, 0, intTemp);
                }
            }

            objOutputStream.close();
            objInputStream.close();
            byte[] arrImageByte = objOutputStream.toByteArray();
            return arrImageByte;
        } catch (Exception ex) {
            return null;
        }
    }

    private void setImageToView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imgResult = (ImageView) findViewById(R.id.imgResult);
                imgResult.setImageBitmap(objImage);
            }
        });
    }

    //endregion
}
