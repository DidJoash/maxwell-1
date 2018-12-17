package com.zendesk.maxwell.row;

import java.io.ByteArrayOutputStream;

public class ThreadLocalBuffer {
	private final ThreadLocal<ByteArrayOutputStream> byteArrayThreadLocal =
			new ThreadLocal<ByteArrayOutputStream>(){
				@Override
				protected ByteArrayOutputStream initialValue() {
					return new ByteArrayOutputStream();
				}
			};

	public ByteArrayOutputStream get() {
		return byteArrayThreadLocal.get();
	}

	public ByteArrayOutputStream reset() {
		ByteArrayOutputStream b = byteArrayThreadLocal.get();
		b.reset();
		return b;
	}

	public String consume() {
		ByteArrayOutputStream b = byteArrayThreadLocal.get();
		String s = b.toString();
		b.reset();
		return s;
	}
}
