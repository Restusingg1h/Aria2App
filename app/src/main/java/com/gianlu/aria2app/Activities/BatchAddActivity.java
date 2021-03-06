package com.gianlu.aria2app.Activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddMetalinkBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddTorrentBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddUriBundle;
import com.gianlu.aria2app.Adapters.AddDownloadBundlesAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BatchAddActivity extends ActivityWithDialog implements AddDownloadBundlesAdapter.Listener {
    public static final int REQUEST_URIS_FILE = 5;
    private final static int REQUEST_URI = 0;
    private final static int REQUEST_TORRENT = 1;
    private final static int REQUEST_TORRENT_FILES = 2;
    private final static int REQUEST_METALINK = 3;
    private static final int REQUEST_METALINK_FILES = 4;
    private AddDownloadBundlesAdapter adapter;
    private RecyclerViewLayout layout;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.addDownload_done:
                done();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void done() {
        List<AddDownloadBundle> bundles = adapter.getBundles();
        if (bundles.isEmpty()) return;

        Bundle analytics = new Bundle();
        analytics.putInt("bundles", bundles.size());
        AnalyticsApplication.sendAnalytics(Utils.ACTION_NEW_BATCH, analytics);

        try {
            showProgress(R.string.gathering_information);
            Aria2Helper.instantiate(this).addDownloads(bundles, new AbstractClient.OnResult<List<String>>() {
                @Override
                public void onResult(@NonNull List<String> result) {
                    dismissDialog();
                    Toaster.with(BatchAddActivity.this).message(R.string.downloadsAddedBatch).extra(result).show();
                    if (!isDestroyed()) onBackPressed();
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    dismissDialog();
                    Toaster.with(BatchAddActivity.this).message(R.string.failedAddingDownloads).ex(ex).show();
                }
            });
        } catch (Aria2Helper.InitializingException ex) {
            Toaster.with(this).message(R.string.failedAddingDownload).ex(ex).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_add);
        setTitle(R.string.batchAdd);

        Button singleUri = findViewById(R.id.batchAdd_singleUri);
        singleUri.setOnClickListener(v -> startActivityForResult(new Intent(BatchAddActivity.this, AddUriActivity.class).putExtra("startedForResult", true), REQUEST_URI));
        Button urisFile = findViewById(R.id.batchAdd_urisFile);
        urisFile.setOnClickListener(v -> openDocument("*/*", "Select some uris files", REQUEST_URIS_FILE));

        Button singleTorrent = findViewById(R.id.batchAdd_singleTorrent);
        singleTorrent.setOnClickListener(v -> startActivityForResult(new Intent(BatchAddActivity.this, AddTorrentActivity.class).putExtra("startedForResult", true), REQUEST_TORRENT));
        Button torrentFiles = findViewById(R.id.batchAdd_torrentFiles);
        torrentFiles.setOnClickListener(v -> openDocument("application/x-bittorrent", "Select some torrent files", REQUEST_TORRENT_FILES));

        Button singleMetalink = findViewById(R.id.batchAdd_singleMetalink);
        singleMetalink.setOnClickListener(v -> startActivityForResult(new Intent(BatchAddActivity.this, AddMetalinkActivity.class).putExtra("startedForResult", true), REQUEST_METALINK));
        Button metalinkFiles = findViewById(R.id.batchAdd_metalinkFiles);
        metalinkFiles.setOnClickListener(v -> openDocument("application/metalink4+xml,application/metalink+xml", "Select some Metalink files", REQUEST_METALINK_FILES));

        layout = findViewById(R.id.batchAdd_list);
        layout.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        adapter = new AddDownloadBundlesAdapter(this, this);
        layout.loadListData(adapter, false);
    }

    private void openDocument(@NonNull final String mime, @NonNull final String text, final int requestCode) {
        AskPermission.ask(this, Manifest.permission.READ_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mime);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, text), requestCode);
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                Toaster.with(BatchAddActivity.this).message(R.string.readPermissionDenied).error(true).show();
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.readExternalStorageRequest_title)
                        .setMessage(R.string.readExternalStorageRequest_base64Message);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            ClipData clip = data.getClipData();
            Uri uri = data.getData();

            try {
                switch (requestCode) {
                    case REQUEST_TORRENT_FILES:
                        if (uri != null) {
                            adapter.addItem(AddTorrentBundle.fromUri(this, uri));
                        } else if (clip != null) {
                            for (int i = 0; i < clip.getItemCount(); i++)
                                adapter.addItem(AddTorrentBundle.fromUri(this, clip.getItemAt(i).getUri()));
                        }
                        return;
                    case REQUEST_METALINK_FILES:
                        if (uri != null) {
                            adapter.addItem(AddMetalinkBundle.fromUri(this, uri));
                        } else if (clip != null) {
                            for (int i = 0; i < clip.getItemCount(); i++)
                                adapter.addItem(AddMetalinkBundle.fromUri(this, clip.getItemAt(i).getUri()));
                        }
                        return;
                    case REQUEST_URIS_FILE:
                        if (uri != null) {
                            adapter.addItems(AddUriBundle.fromUri(this, uri));
                        } else if (clip != null) {
                            for (int i = 0; i < clip.getItemCount(); i++)
                                adapter.addItems(AddUriBundle.fromUri(this, clip.getItemAt(i).getUri()));
                        }
                        return;
                    case REQUEST_URI:
                    case REQUEST_TORRENT:
                    case REQUEST_METALINK:
                        AddDownloadBundle bundle = (AddDownloadBundle) data.getSerializableExtra("bundle");
                        int pos = data.getIntExtra("pos", -1);
                        if (pos == -1) adapter.addItem(bundle);
                        else adapter.itemChanged(pos, bundle);
                        return;
                }
            } catch (AddDownloadBundle.CannotReadException ex) {
                Toaster.with(this).message(R.string.invalidFile).ex(ex).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) layout.showInfo(R.string.noBatchAdd);
        else layout.showList();
    }

    @Override
    public void onEdit(int pos, @NonNull AddDownloadBundle bundle) {
        if (bundle instanceof AddUriBundle) {
            startActivityForResult(new Intent(BatchAddActivity.this, AddUriActivity.class)
                    .putExtra("edit", bundle)
                    .putExtra("startedForEdit", pos), REQUEST_URI);
        } else if (bundle instanceof AddTorrentBundle) {
            startActivityForResult(new Intent(BatchAddActivity.this, AddTorrentActivity.class)
                    .putExtra("edit", bundle)
                    .putExtra("startedForEdit", pos), REQUEST_TORRENT);
        } else if (bundle instanceof AddMetalinkBundle) {
            startActivityForResult(new Intent(BatchAddActivity.this, AddMetalinkActivity.class)
                    .putExtra("edit", bundle)
                    .putExtra("startedForEdit", pos), REQUEST_METALINK);
        }
    }
}
