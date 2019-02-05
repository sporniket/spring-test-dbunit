/*
 * Copyright 2002-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.springtestdbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Tests for {@link TestExecutionListenerChain}.
 *
 * @author Phillip Webb
 */
public class TestExecutionListenerChainTest {

	private InOrder ordered;

	private TestExecutionListenerChain chain;

	private TestContext testContext;

	private TestExecutionListener l1;

	private TestExecutionListener l2;

	@BeforeEach
	public void setup() {
		l1 = mock(TestExecutionListener.class);
		l2 = mock(TestExecutionListener.class);
		ordered = inOrder(l1, l2);
		chain = new TestExecutionListenerChain() {
			@Override
			protected Class<?>[] getChain() {
				return null;
			}

			@Override
			protected List<TestExecutionListener> createChain() {
				return Arrays.asList(l1, l2);
			};
		};
		testContext = mock(TestContext.class);
	}

	@Test
	public void shouldCreateChainFromClasses() throws Exception {
		chain = new TestExecutionListenerChain() {
			@Override
			protected Class<?>[] getChain() {
				return new Class<?>[] { TestListener1.class, TestListener2.class };
			};
		};
		List<TestExecutionListener> list = chain.createChain();
		assertEquals(2, list.size());
		assertTrue(list.get(0) instanceof TestListener1);
		assertTrue(list.get(1) instanceof TestListener2);
	}

	@Test
	public void shouldNotCreateWithIllegalConstructor() throws Exception {
		try {
			chain = new TestExecutionListenerChain() {
				@Override
				protected Class<?>[] getChain() {
					return new Class<?>[] { InvalidTestListener.class };
				};
			};
			fail();
		} catch (IllegalStateException ex) {
			assertEquals("Unable to create chain for classes [class com.github.springtestdbunit."
					+ "TestExecutionListenerChainTest$InvalidTestListener]", ex.getMessage());
		}
	}

	@Test
	public void shouldChainBeforeTestClass() throws Exception {
		chain.beforeTestClass(testContext);
		ordered.verify(l1).beforeTestClass(testContext);
		ordered.verify(l2).beforeTestClass(testContext);
	}

	@Test
	public void shouldChainPrepareTestInstance() throws Exception {
		chain.prepareTestInstance(testContext);
		ordered.verify(l1).prepareTestInstance(testContext);
		ordered.verify(l2).prepareTestInstance(testContext);
	}

	@Test
	public void shouldChainBeforeTestMethod() throws Exception {
		chain.beforeTestMethod(testContext);
		ordered.verify(l1).beforeTestMethod(testContext);
		ordered.verify(l2).beforeTestMethod(testContext);
	}

	@Test
	public void shouldChainAfterTestMethod() throws Exception {
		chain.afterTestMethod(testContext);
		ordered.verify(l2).afterTestMethod(testContext);
		ordered.verify(l1).afterTestMethod(testContext);
	}

	@Test
	public void shouldChainAfterTestMethodEvenOnException() throws Exception {

		doThrow(new IOError(null)).when(l2).afterTestMethod(testContext);

		Assertions.assertThrows(Exception.class, () -> {
			chain.afterTestMethod(testContext);
			ordered.verify(l2).afterTestMethod(testContext);
			ordered.verify(l1).afterTestMethod(testContext);
		});
	}

	@Test
	public void shouldChainAfterTestClass() throws Exception {
		chain.afterTestClass(testContext);
		ordered.verify(l2).afterTestClass(testContext);
		ordered.verify(l1).afterTestClass(testContext);
	}

	@Test
	public void shouldChainAfterTestClassEvenOnException() throws Exception {

		doThrow(new IOException()).when(l2).afterTestClass(testContext);

		Assertions.assertThrows(IOException.class, () -> {
			chain.afterTestClass(testContext);
			ordered.verify(l2).afterTestClass(testContext);
			ordered.verify(l1).afterTestClass(testContext);
		});
	}

	public static class TestListener1 extends AbstractTestExecutionListener {

	}

	public static class TestListener2 extends AbstractTestExecutionListener {

	}

	public static class InvalidTestListener extends AbstractTestExecutionListener {
		public InvalidTestListener(String illegal) {
		}
	}

}
