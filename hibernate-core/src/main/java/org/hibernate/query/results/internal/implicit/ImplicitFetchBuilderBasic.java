/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderBasic implements ImplicitFetchBuilder, FetchBuilderBasicValued {
	private final NavigablePath fetchPath;
	private final BasicValuedModelPart fetchable;
	private final FetchBuilder fetchBuilder;

	public ImplicitFetchBuilderBasic(NavigablePath fetchPath, BasicValuedModelPart fetchable) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		this.fetchBuilder = null;
	}

	public ImplicitFetchBuilderBasic(
			NavigablePath fetchPath,
			BasicValuedModelPart fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final Function<String, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		this.fetchBuilder = fetchBuilderResolver.apply( fetchable.getFetchableName() );
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		if ( fetchBuilder != null ) {
			return (BasicFetch<?>) fetchBuilder.buildFetch(
					parent,
					fetchPath,
					jdbcResultsMetadata,
					domainResultCreationState
			);
		}
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup parentTableGroup = creationStateImpl
				.getFromClauseAccess()
				.getTableGroup( parent.getNavigablePath() );

		final String table = fetchable.getContainingTableExpression();
		final String column;

		// In case of a formula we look for a result set position with the fetchable name
		if ( fetchable.isFormula() ) {
			column = fetchable.getFetchableName();
		}
		else {
			column = fetchable.getSelectionExpression();
		}

		final Expression expression = ResultsHelper.resolveSqlExpression(
				creationStateImpl,
				jdbcResultsMetadata,
				parentTableGroup.resolveTableReference( fetchPath, fetchable, table ),
				fetchable,
				column
		);

		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				expression,
				fetchable.getJdbcMapping().getJdbcJavaType(),
				parent,
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				parent,
				fetchPath,
				fetchable,
				FetchTiming.IMMEDIATE,
				domainResultCreationState,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderBasic(" + fetchPath + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderBasic that = (ImplicitFetchBuilderBasic) o;
		return fetchPath.equals( that.fetchPath )
				&& fetchable.equals( that.fetchable );
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		return result;
	}

	@Override
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		if ( fetchBuilder != null ) {
			consumer.accept( fetchPath.getLocalName(), fetchBuilder );
		}
	}
}
