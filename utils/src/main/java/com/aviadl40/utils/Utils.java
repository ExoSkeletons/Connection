package com.aviadl40.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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

	public enum Directions {
		UP,
		LEFT,
		DOWN,
		RIGHT,
		GOING_UP,
		GOING_LEFT,
		GOING_DOWN,
		GOING_RIGHT,
		;
	}

	public enum LoadState {
		UNLOADED,
		LOADING,
		LOADED,
		;
	}

	public static final Runnable DO_NOTHING = new Runnable() {
		@Override
		public void run() {

		}
	};
	private static final Random RANDOM = new Random();

	public static float round(float f, int decimals) {
		if (decimals <= 0) return (int) f;
		float s = (float) Math.pow(10, decimals);
		return ((int) (f * s)) / s;
	}

	public static boolean isFinite(float f) {
		return !(Float.isNaN(f) || Float.isInfinite(f));
	}

	public static float finite(float f, float defaultValue) {
		if (isFinite(f)) return f;
		return defaultValue;
	}

	public static int randomizeWithProbability(byte[] probArray) {
		int index = 0;
		byte sum = 0, ran;
		for (byte p : probArray)
			sum += p;
		ran = (byte) (RANDOM.nextInt(sum + 1) - 1);
		for (byte p : probArray) {
			if (ran < p)
				break;
			ran -= p;
			index++;
		}
		return index;
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

	public static String capitaliseFirst(String s, String regexSeparator) {
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

	public static String toStringShort(Object o) {
		if (o == null)
			return "null";
		Class c = o.getClass();
		return (c.isAnonymousClass() ? c.getSuperclass().getSimpleName() : c.getSimpleName()) + "@" + Integer.toHexString(o.hashCode());
	}

	public static boolean getBit(int b, int index) {
		return ((b >> index) & 1) == 1;
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

	private Utils() {
	}
}