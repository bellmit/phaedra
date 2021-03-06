package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.DefaultDataCaptureStore;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;


public class SubWellDataPersistor extends BaseDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		String[] keys = getNames(store, DefaultDataCaptureStore.WELL_SWDATA_PREFIX);

		String[] featureNames = Arrays.stream(keys)
				.map(key -> key.substring(key.indexOf('.') + 1))
				.distinct().toArray(i -> new String[i]);
		
		Map<SubWellFeature, Map<Well, Object>> dataToSave = new HashMap<>();
		for (String featureName: featureNames) {
			Map<Well, Object> data = new HashMap<>();
			
			String[] wellIds = Arrays.stream(keys)
					.map(key -> key.substring(0, key.indexOf('.')))
					.distinct().toArray(i -> new String[i]);
			
			for (String wellId: wellIds) {
				final Well well = PlateUtils.getWell(plate, Integer.parseInt(wellId));
				if (well.getStatus() == WellStatus.REJECTED_DATACAPTURE.getCode()) {
					continue;
				}
				String key = String.format("%s%s.%s", DefaultDataCaptureStore.WELL_SWDATA_PREFIX, wellId, featureName);
				Object value = store.readValue(key);
				data.put(well, value);
			}
			
			if (!data.isEmpty()) {
				SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(featureName, ProtocolUtils.getProtocolClass(plate));
				dataToSave.put(feature, data);
			}
		}
		
		if (dataToSave.isEmpty()) return;
		
		SubWellService.getInstance().updateData(dataToSave);
		plate.setSubWellDataAvailable(true);
		PlateService.getInstance().updatePlate(plate);
		
		// If needed, trigger a subwell data calculation.
		boolean triggerSubwellCalculation = ProtocolUtils.getFeatures(plate).stream()
				.anyMatch(f -> f.isCalculated() && CalculationTrigger.SubwellDataChange.toString().equals(f.getCalculationTrigger()));
		if (triggerSubwellCalculation) CalculationService.getInstance().triggerSubWellCalculation(plate);
	}

}
