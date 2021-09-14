package com.aviadl40.connection.game.managers;

import android.support.annotation.Nullable;

import com.aviadl40.connection.Connection;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.Utils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;

@SuppressWarnings({"unchecked", "WeakerAccess"})
public final class ScreenManager {
	public abstract static class ManuallyDisposedUIScreen extends UIScreen {
		public ManuallyDisposedUIScreen(UIScreen prev) {
			super(prev);
		}
	}

	public abstract static class UIScreen extends ScreenAdapter {
		public final Stage ui = new Stage();
		public final UIScreen prev;
		private boolean UIBuilt = false;

		public UIScreen(final UIScreen prev) {
			this.prev = prev;
		}

		protected abstract void buildUI();

		protected void update(float delta) {
			if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) && current() == this)
				if (back())
					AudioManager.newSFXActions("click").play();
			ui.act(delta);
		}

		protected void draw() {
			ui.setDebugAll(Settings.drawBorders && Connection.instance.getScreen() == this);
			ui.draw();
		}

		@Override
		public final void render(float delta) {
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			if (delta > .075f) delta = .075f;
			draw();
			update(delta);
		}

		@Override
		public void show() {
			Gdx.input.setInputProcessor(getInputProcessor());
			if (!UIBuilt) {
				buildUI();
				ui.addListener(CLICK_LISTENER);
				UIBuilt = true;
			}
		}

		@Override
		public void dispose() {
			ui.dispose();
			System.out.println("disposed " + Utils.toStringShort(this));
			if (canDispose(prev))
				prev.dispose();
		}

		public boolean back() {
			if (prev == null) return false;
			setScreen(prev);
			return true;
		}

		protected InputProcessor getInputProcessor() {
			return ui;
		}
	}

	public static final float FADE_TIME = .6f;
	private static final InputListener CLICK_LISTENER = new InputListener() {
		@SuppressWarnings("StatementWithEmptyBody")
		@Override
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			Actor target = event.getTarget();
			Actor parent = target.getParent();

			if (!target.isVisible() || (parent != null && !parent.isVisible()))
				return false;

			if (target instanceof Button) {
				if (((Button) target).isDisabled())
					return false;
			} else if (parent instanceof Button) {
				if (((Button) parent).isDisabled())
					return false;
			} else if (target instanceof SelectBox || target instanceof List || target instanceof Tree) {
			} else
				return false;

			AudioManager.newSFXActions("click").play();
			event.handle();
			return true;
		}
	};
	private static UIScreen next = null;

	private static boolean canDispose(@Nullable Screen screen) {
		if (screen == null) return false;
		if (screen instanceof ManuallyDisposedUIScreen) return false;
		System.out.println("can dispose " + Utils.toStringShort(screen) + "?");
		UIScreen s = current();
		while (true) {
			System.out.print(Utils.toStringShort(s));
			if (s == null) {
				System.out.println("\ndid not find " + Utils.toStringShort(screen));
				return true;
			}
			if (s == screen) {
				System.out.println("\nfound " + Utils.toStringShort(screen));
				return false;
			}
			System.out.print(" -> ");
			s = s.prev;
		}
	}

	public static Screen getCurrent() {
		return Connection.instance.getScreen();
	}

	public static <S extends UIScreen> S current() {
		return (S) getCurrent();
	}

	public static boolean isTransitioning() {
		return next != null;
	}

	public static float transitionDuration() {
		return ScreenManager.isTransitioning()
				? FADE_TIME / 2
				: 0;
	}

	public static void setScreen(UIScreen screen) {
		Screen prev = getCurrent();
		Connection.instance.setScreen(next = screen);
		if (canDispose(prev))
			prev.dispose();
		next = null;
	}

	public static <S extends UIScreen> void fadeIn(final S endScreen, final float duration, final Runnable runAtEnd) {
		next = endScreen;
		setScreen(endScreen);
		endScreen.ui.getRoot().getColor().a = 0;
		endScreen.ui.getRoot().addAction(Actions.sequence(
				Actions.fadeIn(duration),
				Actions.run(runAtEnd)
		));
	}

	public static <S extends UIScreen> void fadeOut(final S endScreen, final float duration, final Runnable runAtEnd) {
		final S startScreen = current();
		next = endScreen;
		Gdx.input.setInputProcessor(null);
		startScreen.ui.getRoot().addAction(
				Actions.sequence(
						Actions.fadeOut(duration),
						Actions.run(new Runnable() {
							@Override
							public void run() {
								setScreen(endScreen);
								runAtEnd.run();
							}
						})
				)
		);
	}

	public static <S extends UIScreen> void fadeOutIn(final S endScreen, final float duration, @Nullable final Runnable runAtEnd) {
		final S startScreen = current();
		next = endScreen;
		Gdx.input.setInputProcessor(null);
		startScreen.ui.getRoot().addAction(
				Actions.sequence(
						Actions.fadeOut(duration),
						Actions.run(new Runnable() {
							@Override
							public void run() {
								setScreen(endScreen);
								if (startScreen.prev != endScreen)
									endScreen.ui.getRoot().getColor().a = 0;
								final SequenceAction fadeIn = Actions.sequence(Actions.fadeIn(duration * MathUtils.clamp(1 - endScreen.ui.getRoot().getColor().a, 0, 1)));
								if (runAtEnd != null) fadeIn.addAction(Actions.run(runAtEnd));
								endScreen.ui.getRoot().addAction(fadeIn);
							}
						})
				)
		);
	}

	public static <S extends UIScreen> void fadeOutIn(final S endScreen, final float duration) {
		fadeOutIn(endScreen, duration, null);
	}

	public static <S extends UIScreen> void fadeOutIn(final S endScreen) {
		fadeOutIn(endScreen, FADE_TIME);
	}
}
