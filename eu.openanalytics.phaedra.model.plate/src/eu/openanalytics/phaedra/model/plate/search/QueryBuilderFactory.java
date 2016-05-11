package eu.openanalytics.phaedra.model.plate.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilderFactory;
import eu.openanalytics.phaedra.base.search.IQueryBuilder;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class QueryBuilderFactory extends AbstractQueryBuilderFactory {
	@Override
	public Set<Class<? extends PlatformObject>> getSupportedTypes() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Experiment.class, Plate.class, Well.class, Compound.class, FeatureValue.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends PlatformObject> IQueryBuilder<T> getBuilder(Class<T> clazz) {
		if (clazz.equals(Experiment.class)) {
			return (IQueryBuilder<T>) new ExperimentQueryBuilder();
		}
		if (clazz.equals(Plate.class)) {
			return (IQueryBuilder<T>) new PlateQueryBuilder();
		}
		if (clazz.equals(Well.class)) {
			return (IQueryBuilder<T>) new WellQueryBuilder();
		}
		if (clazz.equals(Compound.class)) {
			return (IQueryBuilder<T>) new CompoundQueryBuilder();
		}
		if (clazz.equals(FeatureValue.class)) {
			return (IQueryBuilder<T>) new FeatureValueQueryBuilder();
		}

		return null;
	}
}
