import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class PacketTimeline {

	List<TruckDataUnit> mTimeline;
	
	public PacketTimeline(){
		mTimeline = Collections.synchronizedList(new ArrayList<TruckDataUnit>(5000));
	}
	
	
}
