package api.services;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.testng.ISuite;

@Service
@Repository
public class TestNGService {

	ISuite iSuite;
	
	public ISuite getiSuite() {
		return iSuite;
	}

	public void setiSuite(ISuite iSuite) {
		this.iSuite = iSuite;
	}
}
