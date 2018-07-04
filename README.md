# UDPTest
一个通过发送UDP包对远端服务器测速的客户端
*需要服务器在对UDP响应*

##1.使用方式
在ipList中添加需要测速的ip或hostname
在doPing方法调用ping方式的地方设置发包数
在ping方法中制定port，默认为9999

##2.说明
程序忽略的UDP的丢包，在ping(String ip, int count)方法中的packetLoss实为到达率，在appendLog中已改为packetAccess
测速结果仅供参考
示例中的两个host并没有对应的服务，所以检测结果为失败
