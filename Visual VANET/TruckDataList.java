import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TruckDataList {

	ArrayList<TruckDataUnit> truckDataList;
	HashMap<String, Color> platoonIdMap;

	public TruckDataList() {
		// truckDataList = Collections.synchronizedList(new
		// ArrayList<TruckDataUnit>(11));
		truckDataList = new ArrayList<TruckDataUnit>(11);
		for (int i = 0; i <= 11; i++) {
			truckDataList.add(new TruckDataUnit());
		platoonIdMap = new HashMap<String,Color>();
		}
	}
	
	public Color getRandomColor(){
		Random random = new Random();
		return new Color(random.nextInt(255),random.nextInt(255),random.nextInt(255),100);
	}

}
