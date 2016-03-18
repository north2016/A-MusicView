# MusicView

#开源自定义控件系列之六
#自定义音乐播放器
* 代码百分百全注释，看不懂你过来，我保证`不打你`
* Canvas动态绘图，涉及`碰撞检测`(很简单)
* `开源免费`对您有帮助请`加星`


#解释
*代码之前是在联想A820t上用px全写死的，未做适配，导致很多手机无法正展示效果，非常抱歉，现在工作太忙，过一段时间一定把适配版本更新上来，你也可以更该部分参数自行适配。^-^

#提示：

1、改改触发弹射的高度mOnBallHeight

2、542行的
      /*RectF cricle = new RectF(centerX - r, newy + r, centerX + r, newy
					- r);
			canvas.drawOval(cricle, mPaint);*/
			改成：
			canvas.drawCircle(centerX, newy, r, mPaint);

![Alt text](/a.gif)

