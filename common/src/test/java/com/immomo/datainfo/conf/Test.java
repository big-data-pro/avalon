package com.immomo.datainfo.conf;
import com.immomo.datainfo.conf.Configuration;

/**
 * Created by kangkai on 18/2/1.
 */
public class Test {

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        configuration.addResource("avalon-default.xml");
        configuration.getProps();
        configuration.writeXml(System.out);
    }
}
