package com.aviadl40.connection;

import android.support.annotation.Nullable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Utils {
	public enum Extension {
		png,
		mp3,
		adv,
		bin,
		xml;

		@Override
		public String toString() {
			return "." + name();
		}
	}

	public enum LoadState {
		UNLOADED,
		LOADING,
		LOADED,
		;
	}

	public static abstract class KonamiCodeListener extends GestureDetector.GestureAdapter {
		// Up Up Down Down Left Right A B Start

		private static final byte SEQUENCE_END = 9;

		private byte sequenceTracker = 0;

		protected abstract void onComplete();

		private void reset() {
			sequenceTracker = 0;
		}

		private void stepForward() {
			sequenceTracker++;
			if (sequenceTracker == SEQUENCE_END) {
				onComplete();
				reset();
			}
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			byte s = sequenceTracker;
			if (x < Gdx.graphics.getWidth() / 2f || button == 0) { // A
				if (sequenceTracker == 6)
					stepForward();
			}
			if (x > Gdx.graphics.getWidth() / 2f || button == 1) { // B
				if (sequenceTracker == 7)
					stepForward();
			}
			if (s == sequenceTracker)
				reset();
			return false;
		}

		@Override
		public boolean longPress(float x, float y) { // Start
			if (sequenceTracker == 8)
				stepForward();
			return super.longPress(x, y);
		}

		@Override
		public boolean fling(float velocityX, float velocityY, int button) {
			byte s = sequenceTracker;
			if (Math.abs(velocityX / velocityY) > 4) { // X
				if (velocityX < 0) // Left
					if (sequenceTracker == 4)
						stepForward();
				if (velocityX > 0) // Right
					if (sequenceTracker == 5)
						stepForward();
			}
			if (Math.abs(velocityY / velocityX) > 4) { // Y
				if (velocityY < 0) // Up
					if (sequenceTracker == 0 || sequenceTracker == 1)
						stepForward();
				if (velocityY > 0) // Down
					if (sequenceTracker == 2 || sequenceTracker == 3)
						stepForward();
			}
			if (s == sequenceTracker)
				reset();
			return false;
		}
	}

	public static class AnimationActor extends Image {
		private Animation<TextureRegionDrawable> animation = null;

		private float time = 0;
		private boolean paused = false;

		public AnimationActor(Animation<TextureRegionDrawable> animation) {
			setAnimation(animation, true);
		}

		public AnimationActor() {
			this(null);
		}

		public Animation<TextureRegionDrawable> getAnimation() {
			return animation;
		}

		public void setAnimation(final Animation<TextureRegionDrawable> a) {
			setAnimation(a, true);
		}

		public void setAnimation(final Animation<TextureRegionDrawable> a, final boolean restart) {
			if (restart)
				restartAnimation();
			animation = a;
			updateDrawable();
		}

		public float getTime() {
			return time;
		}

		public final int getAnimationFrameIndex() {
			return animation == null ? 0 : animation.getKeyFrameIndex(getTime());
		}

		public void setAnimationFrameIndex(final int frame) {
			setAnimationTime(animation == null ? 0 : frame * animation.getFrameDuration());
		}

		public float getDuration() {
			if (animation == null) return 0;
			return animation.getAnimationDuration();
		}

		public void setAnimationTime(final float time) {
			this.time = time;
		}

		public void setPlayMode(Animation.PlayMode playMode) {
			animation.setPlayMode(playMode);
		}

		public void restartAnimation() {
			time = 0;
		}

		public void pauseAnimation(boolean pause) {
			paused = pause;
		}

		public boolean isAnimationFinished() {
			return (animation != null) && animation.isAnimationFinished(time);
		}

		protected void frameChanged() {
		}

		private void updateDrawable() {
			setDrawable(hasFrame(animation, time) ? animation.getKeyFrame(time) : null);
			layout();
		}

		@Override
		public float getPrefWidth() {
			return animation == null ? 0f : animation.getKeyFrames()[0].getMinWidth();
		}

		@Override
		public float getPrefHeight() {
			return animation == null ? 0f : animation.getKeyFrames()[0].getMinHeight();
		}

		@Override
		public void act(float delta) {
			super.act(delta);
			float temp = time;
			if (!paused)
				setAnimationTime(time + delta);
			if (hasFrame(animation, time) && animation.getKeyFrameIndex(temp) != animation.getKeyFrameIndex(time))
				frameChanged();
			updateDrawable();
		}
	}

	public static class RectangleActor extends Actor implements Disposable {
		private final ShapeRenderer sr = new ShapeRenderer();

		private final Color
				c0 = new Color(),
				c1 = new Color(),
				c2 = new Color(),
				c3 = new Color();

		public RectangleActor(Color color) {
			setColor(color);
		}

		public RectangleActor(Color c0, Color c1, Color c2, Color c3) {
			setC0(c0);
			setC1(c1);
			setC2(c2);
			setC3(c3);
		}

		public RectangleActor() {
		}

		public Color getC0() {
			return c0;
		}

		public void setC0(Color c0) {
			this.c0.set(c0);
		}

		public Color getC1() {
			return c1;
		}

		public void setC1(Color c1) {
			this.c1.set(c1);
		}

		public Color getC2() {
			return c2;
		}

		public void setC2(Color c2) {
			this.c2.set(c2);
		}

		public Color getC3() {
			return c3;
		}

		public void setC3(Color c3) {
			this.c3.set(c3);
		}

		@Override
		public final void draw(Batch batch, float parentAlpha) {
			sr.setProjectionMatrix(batch.getProjectionMatrix());
			sr.setTransformMatrix(batch.getTransformMatrix());
			sr.begin(ShapeRenderer.ShapeType.Filled);
			sr.setColor(batch.getColor());
			draw(sr, parentAlpha);
			sr.end();
		}

		public void draw(ShapeRenderer sr, float parentAlpha) {
			sr.rect(
					getX(), getY(), getWidth(), getHeight(),
					getC0().cpy().mul(1, 1, 1, parentAlpha),
					getC1().cpy().mul(1, 1, 1, parentAlpha),
					getC2().cpy().mul(1, 1, 1, parentAlpha),
					getC3().cpy().mul(1, 1, 1, parentAlpha)
			);
		}

		@Override
		public void dispose() {
			sr.dispose();
		}

		@Override
		public final Color getColor() {
			return getC0();
		}


		@Override
		public void setColor(Color color) {
			setC0(color);
			setC1(color);
			setC2(color);
			setC3(color);
		}


		@Override
		public void setColor(float r, float g, float b, float a) {
			setColor(new Color(r, g, b, a));
		}


	}

	@SuppressWarnings("SuspiciousNameCombination")
	public static class SquareButton extends Button {
		public SquareButton(Skin skin) {
			super(skin);
		}

		public SquareButton(Skin skin, String styleName) {
			super(skin, styleName);
		}

		public SquareButton(Actor child, Skin skin, String styleName) {
			super(child, skin, styleName);
		}

		public SquareButton(Actor child, ButtonStyle style) {
			super(child, style);
		}

		public SquareButton(ButtonStyle style) {
			super(style);
		}

		public SquareButton() {
			super();
		}

		public SquareButton(Drawable up) {
			super(up);
		}

		public SquareButton(Drawable up, Drawable down) {
			super(up, down);
		}

		public SquareButton(Drawable up, Drawable down, Drawable checked) {
			super(up, down, checked);
		}

		public SquareButton(Actor child, Skin skin) {
			super(child, skin);
		}

		public float getSize() {
			return getWidth();
		}

		public void setSize(float size) {
			setSize(size, size);
		}

		public float getScale() {
			return getScaleX();
		}

		@Override
		public void setWidth(float width) {
			setSize(width, width);
		}


		@Override
		public void setHeight(float height) {
			setSize(height, height);
		}


		@Override
		public void setSize(float width, float height) {
			super.setSize(width, width);
		}


		@Override
		public void sizeBy(float width, float height) {
			super.sizeBy(width, width);
		}

		@Override
		public void setScaleX(float scaleX) {
			setScale(scaleX);
		}

		@Override
		public void setScaleY(float scaleY) {
			setScale(scaleY);
		}

		@Override
		public void scaleBy(float scaleX, float scaleY) {
			scaleBy(scaleX);
		}
	}

	public static class ColoredRectangle extends Rectangle {
		private Color color;

		public ColoredRectangle(Color color) {
			super();
			this.color = color;
		}

		public ColoredRectangle() {
			this(Color.WHITE);
		}

		public ColoredRectangle(float x, float y, float width, float height, Color color) {
			super(x, y, width, height);
			this.color = color;
		}

		public ColoredRectangle(float x, float y, float width, float height) {
			this(x, y, width, height, Color.WHITE);
		}

		public ColoredRectangle(Rectangle rect, Color color) {
			super(rect);
			this.color = color;
		}

		public ColoredRectangle(Rectangle rect) {
			this(rect, Color.WHITE);
			color = Color.WHITE;
		}

		public ColoredRectangle(ColoredRectangle rect) {
			super(rect);
			color = rect.color;
		}

		public Color getColor() {
			return color;
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}

	public static boolean hasFrame(Animation<TextureRegionDrawable> animation, float time) {
		return animation != null && !(animation.getPlayMode() == Animation.PlayMode.NORMAL && time < 0);
	}

	public static Animation<TextureRegionDrawable> buildAnimation(AssetManager assetManager, String fileName, int w, int h, float interval, Animation.PlayMode pm) {
		FileHandle file = Gdx.files.internal(fileName + Extension.png);

		//System.out.println(fileName + " (" + ((file.exists()) ? "FOUND" : "MISSING") + ") " + w + "x" + h + " " + pm.name());

		if (!assetManager.isLoaded(file.path(), Texture.class) || w <= 0 || h <= 0 || interval < 0)
			return null;

		Animation<TextureRegionDrawable> animation;
		Texture tx = assetManager.get(file.path(), Texture.class);
		TextureRegionDrawable[] frames;
		TextureRegion[][] tr;
		tr = TextureRegion.split(tx, w, h);
		// Place the regions into a 1D array, starting from the top left, going across.
		frames = new TextureRegionDrawable[((tx.getWidth() / w) * (tx.getHeight() / h))];
		for (int i = 0, index = 0; i < tx.getHeight() / h; i++)
			for (int j = 0; j < tx.getWidth() / w; j++, index++)
				frames[index] = new TextureRegionDrawable(tr[i][j]);

		if (frames.length == 0)
			throw new IllegalArgumentException("could not build " + w + "x" + h + " frames out of " + tx.getWidth() + "x" + tx.getHeight() + " \"" + fileName + "\" sheet.");
		else if (tx.getWidth() % w > 0 || tx.getHeight() % h > 0)
			System.err.println("warning: " + tx.getWidth() + "x" + tx.getHeight() + " sheet \"" + fileName + "\" does not fully split into [" + w + "x" + h + "] frames.");

		// Initialize the Animation with the frame interval and the array of frames and Add it to animations
		animation = new Animation<>(interval, frames);
		animation.setPlayMode(pm);
		return animation;

	}

	static <T> void loadAssetFilesRecursively(AssetManager assetManager, FileHandle dir, Extension extension, Class<T> tClass) {
		for (FileHandle file : dir.list())
			if (file.isDirectory())
				loadAssetFilesRecursively(assetManager, file, extension, tClass);
			else if (file.extension().equals(extension.name()))
				assetManager.load(file.path(), tClass);
	}

	public static float round(float f, int decimals) {
		float s = (float) Math.pow(10, decimals);
		return ((int) (f * s)) / s;
	}

	public static float findX(float V, float T) { // X = VT
		return V * T;
	}

	public static float findV(float X, float T) { // V = X/T
		return X / T;
	}

	public static float findT(float X, float V) { // T = X/V
		return X / V;
	}

	public static void centerX(Actor actor, float XMin, float XMax) {
		actor.setX(XMin + ((XMax - XMin) - actor.getWidth() * actor.getScaleX()) / 2);
	}

	public static void centerY(Actor actor, float YMin, float YMax) {
		actor.setY(YMin + ((YMax - YMin) - actor.getHeight() * actor.getScaleY()) / 2);
	}

	public static void centerXY(Actor actor, float XMin, float XMax, float YMin, float YMax) {
		centerX(actor, XMin, XMax);
		centerY(actor, YMin, YMax);
	}

	public static void centerX(Actor actor) {
		centerX(actor, 0, Gdx.graphics.getWidth());
	}

	public static void centerY(Actor actor) {
		centerY(actor, 0, Gdx.graphics.getHeight());
	}

	public static void centerXY(Actor actor) {
		centerXY(actor, 0, Gdx.graphics.getWidth(), 0, Gdx.graphics.getHeight());
	}

	public static void paste(TextField textField, String text) {
		int i = textField.getCursorPosition();
		textField.cut();
		textField.setText(textField.getText().substring(0, textField.getCursorPosition()) + text + textField.getText().substring(textField.getCursorPosition()));
		textField.setCursorPosition(i + text.length());
	}

	@SafeVarargs
	public static <T> ArrayList<T> toArrayList(T... args) {
		final ArrayList<T> res = new ArrayList<>();
		Collections.addAll(res, args);
		return res;
	}

	public static String toSingleLine(String s) {
		return s.replace('\t', ' ').replace('\n', ' ').replaceAll(" {2}", " ");
	}

	public static <T> int countValue(Array<T> array, T comparedValue, boolean identity) {
		int i = array.size - 1;
		T[] items = array.items;
		int count = 0;

		if (!identity && comparedValue != null) {
			while (i >= 0) {
				if (comparedValue.equals(items[i]))
					count++;
				i--;
			}
		} else {
			while (i >= 0) {
				if (items[i] == comparedValue)
					count++;
				i--;
			}
		}

		return count;
	}

	public static <T> T getLast(Array<T> array) {
		return array.get(array.size - 1);
	}

	public static Array<Class> toClassArray(Array<?> array) {
		Array<Class> res = new Array<>();
		for (Object o : array)
			res.add(o.getClass());
		return res;
	}

	public static String plural(String s) {
		return s.endsWith("s")
				? s
				: (
				s.endsWith("y")
						? s.substring(0, s.length() - 1) + "ies"
						: s + "s"
		);
	}

	public static String amount(String name, int amount, boolean withAmount) {
		return "" + (withAmount ? amount + " " : "") + (Math.abs(amount) == 1 ? name : plural(name));
	}

	public static String capitaliseFirst(String s, @Nullable String regexSeparator) {
		StringBuilder res = new StringBuilder();
		if (regexSeparator != null)
			for (String word : s.split(regexSeparator))
				res.append(capitaliseFirst(word)).append(regexSeparator);
		else
			res.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
		return res.toString();
	}

	public static String capitaliseFirst(String s) {
		return capitaliseFirst(s, null);
	}

	public static String toStringShort(@Nullable Object o) {
		if (o == null)
			return "null";
		Class c = o.getClass();
		return (c.isAnonymousClass() ? c.getSuperclass().getSimpleName() : c.getSimpleName()) + "@" + Integer.toHexString(o.hashCode());
	}

	public static String repeat(String s, int count) {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < count; i++)
			res.append(s);
		return res.toString();
	}

	public static int getNextIndex(Object[] arr, int startIndex) {
		int i = startIndex;
		do
			i = i == arr.length - 1 ? 0 : i + 1;
		while (arr[i] == null && i != startIndex);

		return i;
	}
}