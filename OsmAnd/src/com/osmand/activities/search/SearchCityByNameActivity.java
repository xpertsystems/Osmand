package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.City;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<City> {
	private RegionAddressRepository region;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public List<City> getObjects() {
		List<City> l = new ArrayList<City>();
		if(region != null){
			region.fillWithSuggestedCities("", l, null);
		}
		return l;
	}
	
	@Override
	public void updateTextView(City obj, TextView txt) {
		txt.setText(obj.getName());
	}
	
	@Override
	public void itemSelected(City obj) {
		OsmandSettings.setLastSearchedCity(this, obj.getId());
		finish();
		
	}
}
