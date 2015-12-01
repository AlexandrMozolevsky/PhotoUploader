package freelance.android.erick.photouploader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by erick on 27.11.15.
 */
public class UploadToServer extends Activity {
    String filePath = null;
    Button buttonUpload;
    ProgressBar progressBar;
    int serverResponseCode;
    private Bitmap myBitmap;
    private VideoView videoPreview;
    private ImageView imgPreview;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_upload);
        Intent intent = getIntent();
        filePath = intent.getStringExtra("filePath");
        String type = intent.getStringExtra("type");

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        videoPreview = (VideoView) findViewById(R.id.videoPreview);

        final File imgFile = new File(filePath);

        if (imgFile.exists()) {
            if (type.equals(String.valueOf(MainActivity.MEDIA_TYPE_IMAGE))) {
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imgPreview.setImageBitmap(myBitmap);
            } else if (type.equals(String.valueOf(MainActivity.MEDIA_TYPE_VIDEO))) {
                videoPreview.requestFocus();
                videoPreview.setVisibility(View.VISIBLE);
                videoPreview.setVideoPath(filePath);
                videoPreview.start();
            }
        }

        buttonUpload = (Button) findViewById(R.id.btnUpload);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                new AsyncTask<Void, Integer, Integer>() {
                                                    @Override
                                                    protected Integer doInBackground(Void... params) {
                                                        int result = -100;
                                                        try {
                                                            Uri.Builder builder = new Uri.Builder();
                                                            builder.scheme("http")
                                                                    .authority(Config.SERVER_ADDRESS)
                                                                    .appendPath("user")
                                                                    .appendPath("storePhoto");
                                                            String myUrl = builder.build().toString();

                                                            String lineEnd = "\r\n";
                                                            String twoHyphens = "--";
                                                            String boundary = "*****";
                                                            int bytesRead, bytesAvailable, bufferSize;
                                                            byte[] buffer;
                                                            int maxBufferSize = 100 * 1024 * 1024;
                                                            if (!imgFile.isFile()) {
                                                                Log.e("uploadFile", "Source File Does not exist");
                                                                return 0;
                                                            }

                                                            FileInputStream fileInputStream = new FileInputStream(imgFile);
                                                            URL url = new URL(myUrl);
                                                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                                            conn.setRequestProperty("Authorization", "Basic " + "KEK");
                                                            conn.setDoInput(true);
                                                            conn.setDoOutput(true);
                                                            conn.setUseCaches(false);
                                                            conn.setRequestMethod("POST");
                                                            conn.setRequestProperty("Connection", "Keep-Alive");
                                                            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                                                            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
//                                                            conn.setRequestProperty("photo", imgFile.getName());
                                                            conn.setRequestProperty("Transfer-Encoding", "chunked");

                                                            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                                                            dos.writeBytes(twoHyphens + boundary + lineEnd);
                                                            dos.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"" + imgFile.getName() + "\"" + lineEnd);
                                                            dos.writeBytes(lineEnd);

                                                            bytesAvailable = fileInputStream.available();
                                                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                                            buffer = new byte[bufferSize];

                                                            int count = 0;

                                                            int bufferLenght = 1024;
                                                            for (int i = 0; i < buffer.length; i += bufferLenght) {
                                                                if (i + 1024 > buffer.length) {
                                                                    count += fileInputStream.read(buffer, i, buffer.length - (buffer.length / 1024) * 1024);
                                                                    i += buffer.length - (buffer.length / 1024) * 1024;
                                                                } else {
                                                                    count += fileInputStream.read(buffer, i, bufferLenght);
                                                                }
                                                                publishProgress((int) ((i / (float) buffer.length) * 100));
                                                            }
                                                            dos.write(buffer, 0, count);
                                                           /* while ((count = fileInputStream.read(buffer, 0, bufferSize)) != -1) {
                                                                total += count;
                                                                publishProgress((int) ((total * 100) / imgFile.length()));
                                                                dos.write(buffer, 0, count);
                                                            }*/

                                                            dos.writeBytes(lineEnd);
                                                            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                                                            dos.writeBytes(twoHyphens + boundary + lineEnd);
                                                            dos.writeBytes("Content-Disposition: form-data; name=\"username\"\n");
                                                            dos.writeBytes(lineEnd);
                                                            dos.writeBytes("erick_voodoo");
                                                            dos.writeBytes(lineEnd);
                                                            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                                                            serverResponseCode = conn.getResponseCode();
                                                            String serverResponseMessage = conn.getResponseMessage();

                                                            /*InputStream stream;

                                                            if (serverResponseCode >= HttpStatus.SC_BAD_REQUEST)
                                                                stream = conn.getErrorStream();
                                                            else
                                                                stream = conn.getInputStream();

                                                            String resultString = "";
                                                            String responseLine = "";

                                                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                                                            while ((responseLine = bufferedReader.readLine()) != null) {
                                                                resultString += responseLine;
                                                            }
                                                            bufferedReader.close();*/

                                                            Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
                                                            if (serverResponseCode == 200) {
                                                                runOnUiThread(new Runnable() {
                                                                    public void run() {
                                                                        Toast.makeText(UploadToServer.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }

                                                            fileInputStream.close();
                                                            dos.flush();
                                                            dos.close();
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                        return result;
                                                    }

                                                    @Override
                                                    protected void onProgressUpdate(Integer... values) {
                                                        ((ProgressBar) findViewById(R.id.progressBar)).setProgress(values[0]);
                                                    }
                                                }.execute();
                                            }
                                        }

        );
    }
}
