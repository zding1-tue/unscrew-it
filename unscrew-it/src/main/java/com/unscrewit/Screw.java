package com.unscrewit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * 螺丝：有位置与颜色，可检测点击命中并负责绘制自身.
 */
public class Screw {

    /** 直径（像素）. */
    private static final int SIZE = 18;

    /** 螺丝中心坐标. */
    public final int x;
    public final int y;

    /** 螺丝颜色.*/
    public final Color color;

    /**
     * 构造一个螺丝.
     *
     * @param x      中心 X 坐标（像素）
     * @param y      中心 Y 坐标（像素）
     * @param color  颜色
     */
    public Screw(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    /**
     * 判断点击点是否命中该螺丝（以圆形命中区域为准）.
     *
     * @param p 点击点
     * @return 是否命中
     */
    public boolean contains(Point p) {
        // 用欧氏距离判断是否在半径内
        double dx = p.x - x;
        double dy = p.y - y;
        double r = SIZE / 2.0;
        return dx * dx + dy * dy <= r * r;
    }

    /**
     * 绘制该螺丝的外观（实体圆 + 边框）.
     *
     * @param g2 图形环境
     */
    public void draw(Graphics2D g2) {
        int r = SIZE / 2;
        g2.setColor(color);
        g2.fillOval(x - r, y - r, SIZE, SIZE);

        g2.setColor(Color.DARK_GRAY);
        g2.drawOval(x - r, y - r, SIZE, SIZE);
    }

    /**
     * 获取当前螺丝的直径（像素）.
     *
     * @return 直径
     */
    public static int size() {
        return SIZE;
    }
}

