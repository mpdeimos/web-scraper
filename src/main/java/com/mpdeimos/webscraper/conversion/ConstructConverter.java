package com.mpdeimos.webscraper.conversion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.jsoup.nodes.Element;

import com.mpdeimos.webscraper.ScraperContext;
import com.mpdeimos.webscraper.ScraperError;
import com.mpdeimos.webscraper.ScraperException;

/**
 * Converter for conversion by calling a constructor using either the source
 * element text (default), source {@link Element} or {@link ScraperContext} as
 * argument.
 * 
 * @author mpdeimos
 */
public class ConstructConverter implements Converter
{
	/** Option annotation for {@link ConstructConverter}. */
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Option
	{
		/** The object passed as first argument. */
		public EArgumentType[] value() default EArgumentType.TEXT;

		/** Arbitrary array of further string arguments. */
		public String[] strings() default {};
	}

	/** The type of object passed as first argument to the constructor. */
	public enum EArgumentType
	{
		/** The extracted source text. */
		TEXT(String.class)
		{
			@Override
			protected Object getArgument(ScraperContext context)
			{
				return context.getSourceText();
			}
		},

		/** The current DOM {@link Element}. */
		ELEMENT(Element.class)
		{
			@Override
			protected Object getArgument(ScraperContext context)
			{
				return context.getSourceElement();
			}
		},

		/** The current {@link ScraperContext}. */
		CONTEXT(ScraperContext.class)
		{
			@Override
			protected Object getArgument(ScraperContext context)
			{
				return context;
			}
		};

		/** The class of the object. */
		private final Class<?> clazz;

		/** Constructor. */
		private EArgumentType(Class<?> clazz)
		{
			this.clazz = clazz;
		}

		/** @returns The argument corresponding to the argument type. */
		protected abstract Object getArgument(ScraperContext context);
	}

	/** {@inheritDoc} */
	@Override
	public Object convert(ScraperContext context) throws ScraperException
	{
		EArgumentType[] argumentTypes = { EArgumentType.TEXT };
		String[] args = {};
		if (context.getTargetField().isAnnotationPresent(Option.class))
		{
			Option option = context.getTargetField()
					.getAnnotation(Option.class);
			argumentTypes = option.value();
			args = option.strings();
		}

		ArrayList<Object> argList = new ArrayList<Object>();
		ArrayList<Class<?>> argTypes = new ArrayList<Class<?>>();
		Class<?> targetType = context.getTargetType();

		for (EArgumentType argType : argumentTypes)
		{
			argList.add(argType.getArgument(context));
			argTypes.add(argType.clazz);
		}
		for (String arg : args)
		{
			argList.add(arg);
			argTypes.add(arg.getClass());
		}

		return callConstructor(targetType, argTypes, argList);
	}

	/** Tries to call the constructor with the given arguments. */
	private Object callConstructor(Class<?> targetType,
			ArrayList<Class<?>> argTypes, ArrayList<Object> argList)
			throws ScraperError
	{
		try
		{
			Constructor<?> constructor = targetType.getConstructor(argTypes
					.toArray(new Class<?>[argTypes.size()]));
			return constructor.newInstance(argList.toArray());
		}
		catch (ReflectiveOperationException e)
		{
			throw new ScraperError(
					"Failed creating instance of " + targetType.getCanonicalName(), e); //$NON-NLS-1$
		}
		catch (IllegalArgumentException e)
		{
			throw new ScraperError(
					"Failed creating instance of " + targetType.getCanonicalName(), e); //$NON-NLS-1$
		}
	}
}