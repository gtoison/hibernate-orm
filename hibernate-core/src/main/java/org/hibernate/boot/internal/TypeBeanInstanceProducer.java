/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.InstantiationException;
import org.hibernate.Internal;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.spi.TypeBootstrapContext;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * {@link BeanInstanceProducer} implementation for building beans related to custom types.
 *
 * @author Christian Beikov
 */
@Internal
public class TypeBeanInstanceProducer implements BeanInstanceProducer, TypeBootstrapContext {
	private final ConfigurationService configurationService;

	public TypeBeanInstanceProducer(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		final Constructor<B> bootstrapContextAwareConstructor =
				ReflectHelper.getConstructorOrNull( beanType, TypeBootstrapContext.class );
		if ( bootstrapContextAwareConstructor != null ) {
			try {
				return bootstrapContextAwareConstructor.newInstance( this );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate type", beanType, e );
			}
		}
		else {
			final Constructor<B> constructor = ReflectHelper.getConstructorOrNull( beanType );
			if ( constructor != null ) {
				try {
					return constructor.newInstance();
				}
				catch ( Exception e ) {
					throw new InstantiationException( "Could not instantiate type", beanType, e );
				}
			}
			else {
				throw new InstantiationException( "No appropriate constructor for type", beanType );
			}
		}
	}

	@Override
	public <B> B produceBeanInstance(String name, Class<B> beanType) {
		return produceBeanInstance( beanType );
	}

	@Override
	public Map<String, Object> getConfigurationSettings() {
		return configurationService.getSettings();
	}
}
