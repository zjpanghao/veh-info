package vehinfo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import vehinfo.entity.ConnectInfo;
import vehinfo.service.NetLogService;

@RestController
@Controller
@RequestMapping("/veh")
public class VehController {
    @Autowired
    private NetLogService netLogService;

    @ResponseBody
    @RequestMapping("/conn")
    public String test() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ConnectInfo connectInfo : netLogService.getConnectInfoList()) {
            stringBuilder.append(connectInfo.getIp() + " " + connectInfo.getPort() + " " + connectInfo.getConnTime() + "\n");
        }
        return  stringBuilder.toString();
    }

    @RequestMapping("/core")
    public int core() {
        return netLogService.getCoreSize();
    }

    @RequestMapping("/active")
    public int active() {
        return netLogService.getActiveSize();
    }

    @RequestMapping("/taskCount")
    public long taskCount() {
        return netLogService.getTaskCount();
    }

    @RequestMapping("/largest")
    public long largest() {
        return netLogService.getLargestSize();
    }
}
