package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;

import com.example.bt_test.R;

import androidx.appcompat.content.res.AppCompatResources;

//класс для вертикального слайдера
public class VerticalSeekBar extends SeekBar {

    private int size = 20;

    public VerticalSeekBar(Context context) {
        super(context);
        init(context, null, true);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, true);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, true);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    private void init(Context context, AttributeSet attrs, boolean applyAttributeTheme)
    {
        // поменять графику переключателя
//        PaintDrawable thumb = new PaintDrawable(Color.parseColor("#2C3E50"));
//        thumb.setCornerRadius(size * 9 / 8);
//        thumb.setIntrinsicWidth(size * 9 / 4);
//        thumb.setIntrinsicHeight(size * 9 / 4);
//        setThumb(thumb);
        //прячем переключатель
        setThumb(null);

        // поменять графику прогресса
//        PaintDrawable progress = new PaintDrawable(Color.parseColor("#34495E"));
//        progress.setCornerRadius(size);
//        progress.setIntrinsicHeight(size);
//        progress.setIntrinsicWidth(size);
//        progress.setDither(true);
        //берём графику прогресса из текстуры barfull
        Drawable myprogress = AppCompatResources.getDrawable(context, R.drawable.barfull);
        ClipDrawable progressClip = new ClipDrawable(myprogress, Gravity.LEFT, ClipDrawable.HORIZONTAL);

        // secondary progress
//        PaintDrawable secondary = new PaintDrawable(Color.parseColor("#EBEDEF"));
//        secondary.setCornerRadius(size);
//        secondary.setIntrinsicHeight(size);
//        ClipDrawable secondaryProgressClip = new ClipDrawable(secondary, Gravity.LEFT, ClipDrawable.HORIZONTAL);

        // background
        PaintDrawable background = new PaintDrawable(Color.parseColor("#EBEDEF"));
        background.setCornerRadius(size);
        background.setIntrinsicHeight(size);
//        Drawable mybackground = AppCompatResources.getDrawable(context, R.drawable.barback);
//        mybackground.setBounds(1,1,1,1);

        // применяем собранную графику
        LayerDrawable ld = (LayerDrawable) getProgressDrawable();
        ld.setDrawableByLayerId(android.R.id.background, background);
        ld.setDrawableByLayerId(android.R.id.progress, progressClip);
//        ld.setDrawableByLayerId(android.R.id.secondaryProgress, secondaryProgressClip);
//        setProgress(50);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(),0);

        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                int i=0;
                i=getMax() - (int) (getMax() * event.getY() / getHeight());
                setProgress(i);
                Log.i("Progress",getProgress()+"");
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

}