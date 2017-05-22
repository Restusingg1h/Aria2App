package com.gianlu.aria2app.Activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.AddDownload.Base64Fragment;
import com.gianlu.aria2app.Activities.AddDownload.OptionsFragment;
import com.gianlu.aria2app.Activities.AddDownload.UrisFragment;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;
import java.util.Map;

public class AddTorrentActivity extends AppCompatActivity {
    private ViewPager pager;
    private UrisFragment urisFragment;
    private OptionsFragment optionsFragment;
    private Base64Fragment base64Fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addTorrent);

        Toolbar toolbar = (Toolbar) findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = (ViewPager) findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.addDownload_tabs);

        base64Fragment = Base64Fragment.getInstance(this);
        urisFragment = UrisFragment.getInstance(this, false);
        optionsFragment = OptionsFragment.getInstance(this);

        pager.setAdapter(new PagerAdapter<>(getSupportFragmentManager(), base64Fragment, urisFragment, optionsFragment));
        pager.setOffscreenPageLimit(2);

        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    private void done() {
        String base64 = base64Fragment.getBase64();
        if (base64 == null) {
            CommonUtils.UIToast(this, Utils.ToastMessages.NO_BASE64_FILE);
            pager.setCurrentItem(0, true);
            return;
        }

        List<String> uris = urisFragment.getUris();
        Map<String, String> options = optionsFragment.getOptions();
        Integer position = optionsFragment.getPosition();

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
            return;
        }

        jta2.addTorrent(base64, uris, options, position, new JTA2.IGID() {
            @Override
            public void onGID(String gid) {
                CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.DOWNLOAD_ADDED, gid, new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownload_done:
                done();
                break;
        }

        return true;
    }
}