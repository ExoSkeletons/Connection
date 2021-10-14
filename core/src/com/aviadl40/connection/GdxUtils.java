package com.aviadl40.connection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.utils.Utils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;

import java.io.IOException;
import java.io.Serializable;

@SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
public class GdxUtils {
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
			if (x < (float) Gdx.graphics.getWidth() / 2 || button == 0) { // A
				if (sequenceTracker == 6)
					stepForward();
			}
			if (x > (float) Gdx.graphics.getWidth() / 2 || button == 1) { // B
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
			Animation old = animation;
			animation = a;
			if (restart) restartAnimation();
			if (old != a) frameChanged();
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

		public float getAnimationDuration() {
			if (animation == null) return 0;
			return animation.getAnimationDuration();
		}

		public void setAnimationTime(final float time) {
			this.time = time;
		}

		public void setAnimationPlayMode(Animation.PlayMode playMode) {
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
		@Deprecated
		public final void draw(Batch batch, float parentAlpha) {
			batch.end();
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			sr.setProjectionMatrix(batch.getProjectionMatrix());
			sr.setTransformMatrix(batch.getTransformMatrix());
			sr.begin(ShapeRenderer.ShapeType.Filled);
			sr.setColor(batch.getColor());
			draw(sr, parentAlpha);
			sr.end();
			batch.begin();
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

	public static class ExpandingButton extends WidgetGroup {
		@NonNull
		private final Button root;
		private boolean horizontal = true, positive = true; // +/-
		private float sparsity = 0, progress = 0, animDuration = 0, collapseAfter = -1;
		@NonNull
		private Interpolation interpolation = Interpolation.smoother;

		public ExpandingButton(@NonNull Button root) {
			this.root = root;
			root.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					expand();
				}
			});
		}

		public boolean isHorizontal() {
			return horizontal;
		}

		public void setHorizontal(boolean horizontal) {
			this.horizontal = horizontal;
		}

		public boolean isPositive() {
			return positive;
		}

		public void setPositive(boolean positive) {
			this.positive = positive;
		}

		public float getSparsity() {
			return sparsity;
		}

		public void setSparsity(float sparsity) {
			this.sparsity = sparsity;
		}

		public float getDuration() {
			return animDuration;
		}

		public void setDuration(float duration) {
			this.animDuration = duration;
		}

		public float getCollapseAfter() {
			return collapseAfter;
		}

		public void setCollapseAfter(float collapseAfter) {
			this.collapseAfter = collapseAfter;
		}

		@NonNull
		public Interpolation getInterpolation() {
			return interpolation;
		}

		public void setInterpolation(@NonNull Interpolation interpolation) {
			this.interpolation = interpolation;
		}

		public float getProgress() {
			return progress;
		}

		public void setProgress(float progress) {
			this.progress = progress;
		}

		public void expand() {
			clearActions();
			TemporalAction expandAction = new TemporalAction() {
				{
					setDuration(animDuration);
				}

				@Override
				protected void update(float percent) {
					progress = percent;
				}
			};
			addAction(collapseAfter >= 0
					?
					Actions.sequence(
							expandAction,
							Actions.delay(collapseAfter, Actions.run(new Runnable() {
								@Override
								public void run() {
									collapse();
								}
							}))
					)
					: expandAction
			);
		}

		public void collapse() {
			clearActions();
			addAction(new TemporalAction() {
				{
					setDuration(animDuration);
				}

				@Override
				protected void update(float percent) {
					progress = 1 - percent;
				}
			});
		}

		@Override
		public Actor hit(float x, float y, boolean touchable) {
			if (progress == 0)
				return root.hit(x, y, touchable);
			if (progress == 1)
				return super.hit(x, y, touchable);
			return null;
		}

		@Override
		public void act(float delta) {
			Array<Actor> children = getChildren();
			Actor c, prev = children.first();

			prev.setPosition(0, 0);

			float deltaX, deltaY, alignedX, alignedY;
			for (int i = 0; i < children.size; i++) {
				c = children.get(i);

				alignedX = root.getX() + (root.getWidth() * root.getScaleX() - c.getWidth() * c.getScaleX()) / 2;
				alignedY = root.getY() + (root.getHeight() * root.getScaleY() - c.getHeight() * c.getScaleY()) / 2;

				//*/
				if (horizontal) {
					deltaX = i > 0 ? prev.getX() + interpolation.apply(0, positive ? sparsity + prev.getWidth() * prev.getScaleX() : -sparsity - c.getWidth() * c.getScaleX(), progress) : 0;
					deltaY = alignedY;
				} else {
					deltaX = alignedX;
					deltaY = i > 0 ? prev.getY() + interpolation.apply(0, positive ? sparsity + prev.getHeight() * prev.getScaleY() : -sparsity - c.getHeight() * c.getScaleY(), progress) : 0;
				}

				c.setPosition(deltaX, deltaY);

				/*/
				if (positive) {
					deltaX = horizontal ? sparsity + prev.getWidth() * prev.getScaleX() : 0;
					deltaY = horizontal ? 0 : sparsity + prev.getHeight() * prev.getScaleY();
				} else {
					deltaX = horizontal ? -sparsity - c.getWidth() * c.getScaleX() : 0;
					deltaY = horizontal ? 0 : sparsity - c.getHeight() * c.getScaleY();
				}

				c.setPosition(
						horizontal ? prev.getX() + interpolation.apply(0, deltaX, progress) : alignedX,
						!horizontal ? prev.getY() + interpolation.apply(0, deltaY, progress) : alignedY
				); //*/

				prev = c;
			}

			super.act(delta);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			validate();
			if (isTransform()) applyTransform(batch, computeTransform());
			root.draw(batch, parentAlpha * interpolation.apply(1 - progress));
			drawChildren(batch, parentAlpha);
			if (isTransform()) resetTransform(batch);
		}

		@Override
		protected void drawChildren(Batch batch, float parentAlpha) {
			super.drawChildren(batch, parentAlpha * interpolation.apply(progress));
		}

		@Override
		public void setWidth(float width) {
			root.setWidth(width);
		}

		@Override
		public void setHeight(float height) {
			root.setHeight(height);
		}

		@Override
		public void setSize(float width, float height) {
			super.setSize(width, height);
			root.setSize(width, height);
		}

		@Override
		public void sizeBy(float size) {
			super.sizeBy(size);
			root.sizeBy(size);
		}

		@Deprecated
		public void sizeBy(float width, float height) {
			super.sizeBy(width, height);
			root.sizeBy(width, height);
		}

		@Override
		public float getPrefWidth() {
			if (progress > 0 && horizontal) {
				Actor last = getChild(getChildren().size - 1);
				if (positive)
					return last.getX() + last.getWidth() * last.getScaleX();
				return Math.abs(last.getX());
			}
			return getWidth();
		}

		@Override
		public float getPrefHeight() {
			if (progress > 0 && !horizontal) {
				Actor last = getChild(getChildren().size - 1);
				if (positive)
					return last.getY() + last.getHeight() * last.getScaleY();
				return Math.abs(last.getY());
			}
			return getHeight();
		}
	}

	@SuppressWarnings({"SuspiciousNameCombination", "deprecation"})
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
		@Deprecated
		public void setWidth(float width) {
			setSize(width, width);
		}

		@Override
		@Deprecated
		public void setHeight(float height) {
			setSize(height, height);
		}

		@Override
		@Deprecated
		public void setSize(float width, float height) {
			super.setSize(width, width);
		}

		@Override
		@Deprecated
		public void sizeBy(float width, float height) {
			super.sizeBy(width, width);
		}

		@Override
		public void setScaleX(float scaleX) {
			setScale(scaleX);
		}

		@Override
		@Deprecated
		public void setScaleY(float scaleY) {
			setScale(scaleY);
		}

		@Override
		@Deprecated
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

	public static class LabelNode extends Tree.Node<LabelNode, String, Label> {
		public LabelNode(String text, Label.LabelStyle style) {
			super(new Label(text, style));
			getActor().setAlignment(Align.topLeft);
		}
	}

	public interface XMLSerializable extends Serializable {
		void read(XmlReader.Element e, long saveFormat);

		void write(XmlWriter w) throws IOException;
	}

	public interface AssetLoadListener {
		void onAssetLoad(FileHandle file, Class type);
	}

	public static boolean hasFrame(Animation<TextureRegionDrawable> animation, float time) {
		return animation != null && !(animation.getPlayMode() == Animation.PlayMode.NORMAL && time < 0);
	}

	/**
	 * Creates a TextureRegionDrawable animation from a Texture,
	 * by splitting the Texture into TextureRegions of the given width and height.
	 *
	 * @param tx            Texture to split
	 * @param splitW        Width of final TextureRegion, used for splitting
	 * @param splitH        Height of final TextureRegion, used for splitting
	 * @param frameCount    Number of frames to use, <= 0 to use all
	 * @param frameDuration Animation frame duration
	 * @param pm            Animation PlayMode
	 * @return Result animation
	 * @throws IllegalArgumentException If failed to split using the given width and height.
	 * @see Texture
	 * @see TextureRegion
	 * @see Animation
	 * @see Animation.PlayMode
	 */
	@NonNull
	public static Animation<TextureRegionDrawable> buildAnimation(@NonNull Texture tx, int splitW, int splitH, int frameCount, float frameDuration, Animation.PlayMode pm) {
		TextureRegionDrawable[] frames;
		TextureRegion[][] tr;
		Animation<TextureRegionDrawable> animation;

		tx.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

		// Split texture into regions
		tr = TextureRegion.split(tx, splitW, splitH);
		// Place the regions into a 1D array, starting from the top left, going across.
		frames = new TextureRegionDrawable[frameCount <= 0 ? ((tx.getWidth() / splitW) * (tx.getHeight() / splitH)) : frameCount];
		for (int i = 0, index = 0; i < tx.getHeight() / splitH && index < frames.length; i++)
			for (int j = 0; j < tx.getWidth() / splitW && index < frames.length; j++, index++) {
				frames[index] = new TextureRegionDrawable(tr[i][j]);
			}

		if (frames.length == 0)
			throw new IllegalArgumentException("could not build " + splitW + "x" + splitH + " frames out of " + tx.getWidth() + "x" + tx.getHeight() + " sheet.");

		if (tx.getWidth() % splitW > 0 || tx.getHeight() % splitH > 0)
			System.err.println("warning: " + tx.getWidth() + "x" + tx.getHeight() + " sheet does not fully split into [" + splitW + "x" + splitH + "] frames.");

		animation = new Animation<>(frameDuration, frames);
		animation.setPlayMode(pm);
		return animation;

	}

	/**
	 * Creates a TextureRegionDrawable animation from an asset png file,
	 * by splitting the Texture into TextureRegions based on the given width and height.
	 *
	 * @param assetManager  Asset manager to get asset from
	 * @param assetPath     Path to the asset file
	 * @param splitW        Width of final TextureRegion, used for splitting
	 * @param splitH        Height of final TextureRegion, used for splitting
	 * @param frameCount    Number of frames to use, <= 0 to use all
	 * @param frameDuration Animation frame duration
	 * @param pm            Animation PlayMode
	 * @return Result animation, null if asset does not exist.
	 * @see AssetManager
	 * @see TextureRegion
	 * @see Animation
	 * @see Animation.PlayMode
	 */
	@Nullable
	public static Animation<TextureRegionDrawable> buildAnimation(@NonNull AssetManager assetManager, @NonNull String assetPath, int splitW, int splitH, int frameCount, float frameDuration, @NonNull Animation.PlayMode pm) {
		FileHandle file = Gdx.files.internal(assetPath + com.aviadl40.utils.Utils.Extension.png);

		if (!assetManager.isLoaded(file.path(), Texture.class))
			return null;

		return buildAnimation(assetManager.get(file.path(), Texture.class), splitW, splitH, frameCount, frameDuration, pm);
	}

	/**
	 * Load all files and sub-files recursively from a given directory into the given AssetManager.
	 *
	 * @param assetManager      Asset manager to use for loading.
	 * @param dir               Target directory.
	 * @param extension         {@link com.aviadl40.utils.Utils.Extension} to use for file filtering, leave null for all files.
	 * @param type              Class type of asset to load.
	 * @param assetLoadListener {@link AssetLoadListener} to fire for each file loaded
	 */
	static <T> void loadAssetFilesRecursively(@NonNull AssetManager assetManager, @NonNull FileHandle dir, @Nullable com.aviadl40.utils.Utils.Extension extension, @NonNull Class<T> type, @Nullable AssetLoadListener assetLoadListener) {
		for (FileHandle file : dir.list())
			if (file.isDirectory())
				loadAssetFilesRecursively(assetManager, file, extension, type, assetLoadListener);
			else if (extension == null || file.extension().equals(extension.name())) {
				String path = file.path();
				if (assetLoadListener != null) assetLoadListener.onAssetLoad(file, type);
				if (!assetManager.isLoaded(path, type)) assetManager.load(path, type);
			}
	}

	static <T> void loadAssetFilesRecursively(AssetManager assetManager, FileHandle dir, com.aviadl40.utils.Utils.Extension extension, Class<T> tClass) {
		for (FileHandle file : dir.list())
			if (file.isDirectory())
				loadAssetFilesRecursively(assetManager, file, extension, tClass);
			else if (file.extension().equals(extension.name()))
				assetManager.load(file.path(), tClass);
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

	public static boolean isOnScreen(Actor a) {

			/*

              H  |    H    |  H
            -----+---------+-----
              H  |    V    |  H
            -----+---------+-----
              H  |    H    |  H

            */

		return a.getX() + a.getWidth() * a.getScaleX() >= 0 &&
				a.getX() <= Gdx.graphics.getWidth() &&
				a.getY() + a.getHeight() * a.getScaleY() >= 0 &&
				a.getY() <= Gdx.graphics.getHeight();
	}

	public static void setAlignmentAll(SelectBox selectBox, int align) {
		selectBox.setAlignment(align);
		selectBox.getList().setAlignment(align);
	}

	public static void performClick(Actor actor) {
		Array<EventListener> listeners = actor.getListeners();
		for (int i = 0; i < listeners.size; i++)
			if (listeners.get(i) instanceof ClickListener)
				((ClickListener) listeners.get(i)).clicked(null, 0, 0);
	}

	public static float getFiniteFloatAttribute(XmlReader.Element e, String name, float defaultValue) {
		return com.aviadl40.utils.Utils.finite(e.getFloatAttribute(name, Float.NaN), defaultValue);
	}

	public static void paste(TextField textField, String text) {
		int i = textField.getCursorPosition();
		textField.cut();
		textField.setText(textField.getText().substring(0, textField.getCursorPosition()) + text + textField.getText().substring(textField.getCursorPosition()));
		textField.setCursorPosition(i + text.length());
	}

	@SafeVarargs
	public static <V, A extends Actor, N extends Tree.Node<N, V, A>, T extends Tree<N, V>> T addAll(T t, N... nodes) {
		for (N node : nodes)
			t.add(node);
		return t;
	}

	@SafeVarargs
	public static <V, A extends Actor, N extends Tree.Node<N, V, A>, T extends Tree<N, V>> N addAll(N n, N... nodes) {
		for (N node : nodes)
			n.add(node);
		return n;
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

	public static void resetCamera(Camera camera) {
		camera.direction.set(0, 0, -1);
		camera.up.set(0, 1, 0);
		camera.update();
	}

	public static Animation<TextureRegionDrawable> buildAnimation(AssetManager assetManager, String fileName, int w, int h, float interval, Animation.PlayMode pm) {
		FileHandle file = Gdx.files.internal(fileName + Utils.Extension.png);

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

	private GdxUtils() {
	}
}
