package com.example.test.data;

import com.example.test.bean.AppUpdate;
import com.example.test.utils.device.LocalApps;

import org.json.JSONObject;

import java.util.List;


/**
 * 更新接口数据处理
 *  
 *
 */
public class UpremindDataProcessor extends DataProcessor implements IDataConstant {

	private static final String JSON_RESULT = "result";
	private static final int DATA_RESULT_OK = 200;
	
	@Override
	public DataCenter.Response processData(int dataId, byte[] dataBytes) throws Exception {
		if (dataBytes == null)
			return null;
		JSONObject jsonObj = new JSONObject(new String(dataBytes));
		DataCenter.Response resp = new DataCenter.Response();
		int result = jsonObj.getInt(JSON_RESULT);
		if (result != DATA_RESULT_OK) {
			resp.mSuccess = false;
			return null;
		}
		
		resp.mData = buildData(dataId, jsonObj);
		resp.mSuccess = true;
		return resp;
	}

	@Override
	protected IDataBase buildData(int dataId, JSONObject jsonObj) throws Exception {
		switch (dataId) {
		case UPDATE_INFO:
			UpdateInfo ui = new UpdateInfo();
			ui.readFromJSON(jsonObj);
			fillLocalVersion(ui.mUpdates);
			fillLocalVersion(ui.mIgnores);
			return ui;
		default:
			return null;
		}
	}

	private void fillLocalVersion(List<AppUpdate> list) {
		if (list == null || list.size() == 0)
			return;

		DataCenter dc = DataCenter.getInstance();
		for (AppUpdate info : list) {
			if (info == null)
				continue;
			LocalApps.LocalAppInfo localInfo = dc.requestLocalPackage(info.mPackageName);
			if (localInfo == null)
				continue;
			info.mLocalVersion = localInfo.mVersion;
		}
	}
}
