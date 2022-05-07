package com.dzgu.xrpc.codec;

import com.dzgu.xrpc.consts.RpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import static com.dzgu.xrpc.consts.RpcConstants.*;

/**
 * custom protocol decoder
 * 0     1     2     3     4        5     6     7     8    9          10      11     12     13    14   15   16
 * +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+----+---+
 * |   magic   code        |version |      full length    | messageType| codec|compress|    RequestId       |
 * +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 * |                                                                                                       |
 * |                                         body                                                          |
 * |                                                                                                       |
 * |                                        ... ...                                                        |
 * +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 *
 * @description: 拆包
 * @Author： dzgu
 * @Date： 2022/4/22 15:41
 */
@Slf4j
public class Spliter extends LengthFieldBasedFrameDecoder {
    public Spliter() {
        this(MAX_FRAME_LENGTH, MAGIC_LENGTH + VERSION_LENGTH, FULL_LENGTH_LENGTH,
                -(MAGIC_LENGTH + VERSION_LENGTH + FULL_LENGTH_LENGTH), 0);
    }

    /**
     * @param maxFrameLength      指定包的最大长度，如果超过，直接丢弃
     * @param lengthFieldOffset   描述长度的字段在第几个字节
     * @param lengthFieldLength   length 字段本身的长度(几个字节)
     * @param lengthAdjustment    包的总长度调整，去掉lengthFieldOffset+lengthFieldLength
     * @param initialBytesToStrip 跳过的字节数，识别出整个数据包之后，截掉 initialBytesToStrip之前的数据
     */
    public Spliter(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            if (frame.readableBytes() >= RpcConstants.HEAD_LENGTH) {
                //拒绝非本协议连接
                if(!checkMagicNumberAndVersion(frame)){
                    ctx.channel().close();
                    return null;
                }
            }
        }
        return decoded;


    }

    /**
     * 读取并检查魔数和版本是否符合规定
     */
    private boolean checkMagicNumberAndVersion(ByteBuf in) {
        // 读取魔数
        byte[] bytes = new byte[MAGIC_LENGTH];
        in.readBytes(bytes);
        // 比较魔数是否符合规定，不符合抛出异常
        for (int i = 0; i < MAGIC_LENGTH; i++) {
            if (bytes[i] != RpcConstants.MAGIC_NUMBER[i]) {
                log.error("Unknown magic code: " + Arrays.toString(bytes));
                return false;
            }
        }
        // 读取版本号
        byte version = in.readByte();
        // 比较版本号是否符合规定，不符合抛出异常
        if (version != RpcConstants.VERSION) {
            log.error("version isn't compatible" + version);
            return false;
        }
        return true;
    }
}
