package ca.pkay.rcloneexplorer.RemoteConfig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import ca.pkay.rcloneexplorer.Activities.MainActivity;
import ca.pkay.rcloneexplorer.R;
import ca.pkay.rcloneexplorer.Rclone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AuthScreenTouchTest {
    @Test public void cancelReceivesTouchOnPhone() { checkCancelTouch(400); }
    @Test public void cancelReceivesTouchOnTablet() { checkCancelTouch(800); }

    private void checkCancelTouch(int widthDp) {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context target = InstrumentationRegistry.getTargetContext();
        Activity activity = instrumentation.startActivitySync(new Intent(target, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        AtomicInteger clicks = new AtomicInteger();
        ConfigCreate[] task = new ConfigCreate[1];
        try {
            instrumentation.runOnMainSync(() -> {
                Configuration config = new Configuration(target.getResources().getConfiguration());
                config.smallestScreenWidthDp = widthDp;
                config.screenWidthDp = widthDp;
                Context context = new ContextThemeWrapper(activity.createConfigurationContext(config), R.style.AppTheme);
                ViewGroup root = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.remote_config_form, null);
                activity.setContentView(root);
                View form = root.findViewById(R.id.form);
                View auth = root.findViewById(R.id.auth_screen);
                View finish = root.findViewById(R.id.finish);
                View cancel = root.findViewById(R.id.cancel_auth);
                cancel.setOnClickListener(v -> clicks.incrementAndGet());
                task[0] = new ConfigCreate(new ArrayList<>(), form, auth, finish, context, new Rclone(context));
                // Exercise onPreExecute without running OAuth or changing any remote.
                task[0].executeOnExecutor(command -> {});
                assertEquals(View.GONE, form.getVisibility());
                assertEquals(View.GONE, finish.getVisibility());
                assertEquals(View.VISIBLE, auth.getVisibility());
                int width = (int) (widthDp * context.getResources().getDisplayMetrics().density);
                int height = (int) (900 * context.getResources().getDisplayMetrics().density);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, width, height);
                Rect bounds = new Rect();
                cancel.getDrawingRect(bounds);
                root.offsetDescendantRectToMyCoords(cancel, bounds);
                assertTrue("Cancel must have visible bounds", bounds.width() > 0 && bounds.height() > 0);
                long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
                MotionEvent up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
                try {
                    assertTrue("Layout must consume touch down", root.dispatchTouchEvent(down));
                    assertTrue("Layout must consume touch up", root.dispatchTouchEvent(up));
                } finally {
                    down.recycle();
                    up.recycle();
                }
            });
            instrumentation.waitForIdleSync();
            assertEquals("Cancel must receive the touch through the actual layout", 1, clicks.get());
        } finally {
            instrumentation.runOnMainSync(() -> {
                if (task[0] != null) task[0].cancel(false);
                activity.finish();
            });
        }
    }
}
