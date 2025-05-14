// ColorSeekBar.java
package cn.hellokk.ble.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * 作者: Kun on 2025/5/10.
 * 邮箱: vip@hellokk.cc.
 * 描述: 颜色控制 SeekBar
 */
import androidx.appcompat.widget.AppCompatSeekBar;

import cn.hellokk.ble.R;

public class ColorSeekBar extends AppCompatSeekBar {
    private Paint paint;
    private int color;

    public ColorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setMax(255);
        setThumb(getResources().getDrawable(R.drawable.seekbar_thumb)); // 设置滑块
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // 创建渐变色
        LinearGradient gradient = new LinearGradient(0, 0, width, 0,
                new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF},
                null, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, paint);
    }

    @Override
    public void setProgress(int progress) {
        super.setProgress(progress);
        int r = progress;
        int g = 255 - r; // 示例变化
        int b = (int) (Math.sin((r / 255.0) * Math.PI) * 255); // 示例变化
        color = android.graphics.Color.rgb(r, g, b);
    }

    public int getColor() {
        return color;
    }
}