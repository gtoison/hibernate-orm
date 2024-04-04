/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * A model part that is (or can be) embeddable-valued (composite) - {@linkplain JaxbEmbeddedIdImpl},
 * {@linkplain JaxbEmbeddedIdImpl} and {@linkplain JaxbElementCollectionImpl}
 *
 * @author Steve Ebersole
 */
public interface JaxbEmbeddedMapping extends JaxbSingularAttribute {
	String getTarget();
	void setTarget(String target);
}