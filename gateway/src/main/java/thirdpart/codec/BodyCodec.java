package thirdpart.codec;

import com.alipay.remoting.serialization.SerializerManager;
import thirdpart.checksum.ByteCheckSum;


public class BodyCodec implements IBodyCodec {
    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        //1. jdk 序列化
        //2. json
        //3.自定义算法（Hessian2）
        return SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        return SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(bytes,clazz.getName());
    }

}
