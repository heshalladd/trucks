

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TruckDataList {

	ArrayList<TruckDataUnit> truckDataList;
	
	public TruckDataList(){
		//truckDataList = Collections.synchronizedList(new ArrayList<TruckDataUnit>(11));
		truckDataList = new ArrayList<TruckDataUnit>(11);
		for(int i = 0;i<=11;i++){
			truckDataList.add(new TruckDataUnit());
		}
	}

}
