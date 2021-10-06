package com.aviadl40.connection;

import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

@SuppressWarnings("WeakerAccess")
public final class Gui implements Disposable {
	public static final class Buttons {
		public static final Utils.SquareButton
				home,
				back;

		static {
			home = new Utils.SquareButton();
			back = new Utils.SquareButton();

			home.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					Connection.instance.home();
				}
			});
			back.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					ScreenManager.current().back();
				}
			});
		}

		private static void update() {
			// Update size
			home.setSize(Gui.buttonSize());
			back.setSize(Gui.buttonSize());
			// Update pos
			back.setPosition(Gui.sparsity(), Gdx.graphics.getHeight() - Gui.sparsity() - back.getSize());
		}
	}

	public static class ButtonStyles {
		public final ButtonStyle
				home,
				prev;

		ButtonStyles(Skin skin) {
			home = skin.get("home", ButtonStyle.class);
			prev = skin.get("back", ButtonStyle.class);
		}
	}

	public static class LabelStyles {
		public final LabelStyle
				titleStyle,
				headerStyle,
				subTextStyle;
		private final LabelStyle
				bodyStyle;

		LabelStyles(Skin skin) {
			titleStyle = new LabelStyle(buildFont(FONT, 48), Color.WHITE);
			headerStyle = new LabelStyle(buildFont(FONT, 44), Color.WHITE);
			bodyStyle = new LabelStyle(buildFont(FONT, 28), Color.WHITE);
			subTextStyle = new LabelStyle(buildFont(FONT, 18), Color.WHITE);
		}
	}

	public static final float BASE_SCALE = 15;
	public static final Color BG = new Color(95f / 255, 157f / 255, 249f / 255, 1f);
	static final String SKIN_PATH = "ui/uiskin.json";
	private static final String FONT = "8BitBold";
	public static ShapeRenderer SR;
	private static Gui instance;

	public static Gui instance() {
		return instance == null ? instance = new Gui() : instance;
	}

	static void reload(Skin skin) {
		(instance = new Gui()).setSkin(skin);
		Buttons.update();
	}

	public static float sparsity() {
		return getScale() * 1.5f;
	}

	public static float buttonSize() {
		return getScale() * 8.65f;
	}

	public static float buttonSizeSmall() {
		return buttonSize() * .35f;
	}

	public static float getScale() {
		return /*/ 5 /*/ Gdx.graphics.getHeight() / 52f /**/;
	}

	static float getScaleFactor() {
		return getScale() / BASE_SCALE;
	}

	private static int fontSize(float size) {
		return (int) Math.min(size, 120);
	}

	public static BitmapFont buildFont(String fontName, int size) {
		BitmapFont font;
		FreeTypeFontGenerator ftfGen;
		try {
			ftfGen = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/" + fontName + ".ttf"));
		} catch (GdxRuntimeException e) {
			e.printStackTrace();
			return null;
		}
		FreeTypeFontGenerator.FreeTypeFontParameter ftfPar = new FreeTypeFontGenerator.FreeTypeFontParameter();
		ftfPar.size = fontSize((size * getScaleFactor() / 2) * 2);
		font = ftfGen.generateFont(ftfPar);
		ftfGen.dispose();
		return font;
	}

	public static Skin skin() {
		return instance().skin;
	}

	static void disposeBatches() {
		if (SR != null) {
			SR.dispose();
			SR = null;
		}
	}

	static void recreateBatches() {
		disposeBatches();
		SR = new ShapeRenderer();
	}

	public static float sparsityBig() {
		return sparsity() * 2;
	}

	public ButtonStyles buttonStyles;
	public LabelStyles labelStyles;
	private Skin skin;

	public void setSkin(Skin newSkin) {
		if (skin != null)
			skin.dispose();

		skin = newSkin;

		buttonStyles = new ButtonStyles(skin);
		labelStyles = new LabelStyles(skin);

		Buttons.home.setStyle(buttonStyles.home);
		Buttons.back.setStyle(buttonStyles.prev);

		final BitmapFont defFont = labelStyles.bodyStyle.font;

		skin.get(LabelStyle.class).font =
				skin.get(CheckBox.CheckBoxStyle.class).font =
						skin.get(TextButton.TextButtonStyle.class).font =
								skin.get("dimmed", List.ListStyle.class).font =
										skin.get(SelectBox.SelectBoxStyle.class).font =
												defFont;
		skin.get(TextField.TextFieldStyle.class).font = buildFont("notes", 32);
		skin.get(Window.WindowStyle.class).titleFont = defFont;
		skin.get(List.ListStyle.class).font = buildFont(FONT, 48);
	}

	@Override
	public void dispose() {
		if (skin != null) {
			skin.dispose();
			skin = null;
		}
	}
}
