//
// Created by Rqg on 2019-08-20.
//

#include <rapidjson/writer.h>
#include <rapidjson/stringbuffer.h>
#include <rapidjson/document.h>
#include <jni.h>
#include <anti_spoof.h>
#include <opencv2/opencv.hpp>
#include "log.h"
#include "httplib.h"

#define LOG_TAG "alg"

JavaVM *acsVm = nullptr;
jclass cbClazz = nullptr;

jmethodID verifyResultCb = nullptr;

jmethodID bmDetectAlive = nullptr;

jmethodID faceQualityResultCb = nullptr;

jclass frameDataClass_g = nullptr;

httplib::Server svr;

std::string benchamrkCacheDir;

#define PREPARE_JNI_ENV JNIEnv *cbEnv;\
    acsVm->AttachCurrentThread(&cbEnv, nullptr);

void result_cb(uint32_t action, uint32_t result, const struct TY_AP_FRAME *frame) {
    ALOGV("result_cb");
    PREPARE_JNI_ENV
    if (verifyResultCb) {
//        cbEnv->CallStaticVoidMethod(cbClazz, verifyResultCb, action, result, NULL);
        jobject jframe = NULL;
        if (frameDataClass_g == NULL) {
            ALOGE("FindClass NO1");
        } else {
            ALOGE("ty_ap_get_face_quality");
            jmethodID mid_cat_init = (cbEnv)->GetMethodID(frameDataClass_g, "<init>", "(II[BJI)V");
            ALOGE("ty_ap_get_face_quality1");


            // 2、获取Cat的构造方法ID(构造方法的名统一为：<init>)
            jint width = frame->width;
            jint height = frame->height;

            jbyte *frame_data = (jbyte *) frame->frame_data;
            int length = width * height * 3;
            jbyteArray jarray = cbEnv->NewByteArray(length);
            cbEnv->SetByteArrayRegion(jarray, 0, length, (jbyte *) frame->frame_data);
            cbEnv->ReleaseByteArrayElements(jarray, cbEnv->GetByteArrayElements(jarray, JNI_FALSE),
                                            0);

            jlong timestamp_millis = frame->timestamp_millis;
            jint eof = frame->eof;
            ALOGE("frame-length:%d", length);
            ALOGE("frame:%d", width);
            ALOGE("frame:%d", height);
            jframe = cbEnv->NewObject(frameDataClass_g, mid_cat_init, width, height, jarray,
                                      timestamp_millis, eof);
        }

        cbEnv->CallStaticVoidMethod(cbClazz, verifyResultCb, action, result, jframe);
    }

    //这里是演示如何使用人脸质量检测接口， 请根据自己的需求使用
//    {
//        float blur = 0.0f;
//        float illu = 0.0f;
//        if (ty_ap_get_face_quality(*frame, &blur, &illu) == 0) {
//            ALOGD("face quality, blur:%f, illumination:%f", blur, illu);
//        } else {
//            ALOGE("get face quality fail");
//        }

//        if (faceQualityResultCb) {
//            if (ty_ap_get_face_quality(*frame, &blur, &illu) == 0) {
//                cbEnv->CallStaticVoidMethod(cbClazz, faceQualityResultCb, true, blur, illu);
//            } else {
//                cbEnv->CallStaticVoidMethod(cbClazz, faceQualityResultCb, false, 0, 0);
//            }
//        }
//
//        struct TY_AP_FACE_RECT rect{};
//        if (ty_ap_get_face_rect(*frame, &rect) == 0) {
//            ALOGD("get face rect: count:%d, [(%d, %d), (%d, %d)]\n", rect.face_count, rect.left,
//                  rect.top, rect.right, rect.bottom);
//        } else {
//            ALOGE("detect face rect fail");
//        }
//    }
}


void frame_finish_cb(const TY_AP_FRAME *frame) {
    free(frame->frame_data);
}


bool run_get_face_rect = true;

void get_face_rect() {
//    while (run_get_face_rect) {
//        auto rect = ty_ap_get_face_rect();
//        if (rect->face_count > 0) {
//            ALOGD("get face rect: count:%d, [(%d, %d), (%d, %d)]\n", rect->face_count, rect->left,
//                  rect->top, rect->right, rect->bottom);
//        }
//        std::this_thread::sleep_for(std::chrono::milliseconds(200));
//    }
}

jboolean
get_face_rect_info(JNIEnv *env, jclass clz, jobject _buf, jint rotation, jint width, jint height,
                   jlong ts, jboolean eof) {
    auto yv12_buf = env->GetDirectBufferAddress(_buf);

    auto *frame_buf = static_cast<uint8_t *>(malloc(width * height * 3));
    cv::Mat _yuv(height + height / 2, width, CV_8UC1, (uchar *) yv12_buf);

    cv::Mat bgrImg((rotation == 2 || rotation == 3) ? width : height,
                   (rotation == 2 || rotation == 3) ? height : width,
                   CV_8UC3, frame_buf);

    switch (rotation) {
        case 1: {
            cv::cvtColor(_yuv, bgrImg, cv::COLOR_YUV2BGR_YV12);
        }
            break;
        case 2: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 1);
        }
            break;
        case 3: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 0);
        }
            break;
        case 4:
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::flip(tmp, bgrImg, -1);
            break;
    }

    TY_AP_FRAME frame{};
    frame.frame_data = frame_buf;
    frame.width = bgrImg.cols;
    frame.height = bgrImg.rows;
    frame.timestamp_millis = ts;
    frame.eof = eof == JNI_TRUE ? 1 : 0;

    jmethodID jmethodId = env->GetStaticMethodID(clz, "faceBoundaryResultCb", "(IIIII)V");

    struct TY_AP_FACE_RECT rect{};
    if (0 == ty_ap_get_face_rect(frame, &rect)) {
        env->CallStaticVoidMethod(clz, jmethodId, rect.face_count, rect.left, rect.top, rect.right,
                                  rect.bottom);
        ALOGD("get face rect: count:%d, [(%d, %d), (%d, %d)]\n", rect.face_count, rect.left,
              rect.top, rect.right, rect.bottom);
        free(frame_buf);
        return JNI_TRUE;
    } else {
        env->CallStaticVoidMethod(clz, jmethodId, 0, -1, -1, -1, -1);
        free(frame_buf);
        ALOGE("detect face rect fail");
        return JNI_FALSE;
    }
}


jboolean
get_face_quality(JNIEnv *env, jclass clz, jobject _buf, jint rotation, jint width, jint height,
                 jlong ts, jboolean eof) {
    auto yv12_buf = env->GetDirectBufferAddress(_buf);

    auto *frame_buf = static_cast<uint8_t *>(malloc(width * height * 3));
    cv::Mat _yuv(height + height / 2, width, CV_8UC1, (uchar *) yv12_buf);

    cv::Mat bgrImg((rotation == 2 || rotation == 3) ? width : height,
                   (rotation == 2 || rotation == 3) ? height : width,
                   CV_8UC3, frame_buf);

    switch (rotation) {
        case 1: {
            cv::cvtColor(_yuv, bgrImg, cv::COLOR_YUV2BGR_YV12);
        }
            break;
        case 2: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 1);
        }
            break;
        case 3: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 0);
        }
            break;
        case 4:
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::flip(tmp, bgrImg, -1);
            break;
    }

    TY_AP_FRAME frame{};
    frame.frame_data = frame_buf;
    frame.width = bgrImg.cols;
    frame.height = bgrImg.rows;
    frame.timestamp_millis = ts;
    frame.eof = eof == JNI_TRUE ? 1 : 0;

    jmethodID jmethodId = env->GetStaticMethodID(clz, "faceQualityResultCb", "(ZFF)V");

    float blur = 0.0f;
    float illu = 0.0f;
    if (ty_ap_get_face_quality(frame, &blur, &illu) == 0) {
        env->CallStaticVoidMethod(clz, jmethodId, true, blur, illu);
        free(frame_buf);
        return JNI_TRUE;
    } else {
        env->CallStaticVoidMethod(clz, jmethodId, false, blur, illu);
        free(frame_buf);
        return JNI_FALSE;
    }
}


jboolean init_anti_spoof(JNIEnv *env, jclass clz, jstring _model_path) {
    auto model_path = env->GetStringUTFChars(_model_path, nullptr);
    cbClazz = (jclass) env->NewGlobalRef(clz);
    env->GetJavaVM(&acsVm);
    verifyResultCb = env->GetStaticMethodID(cbClazz, "verifyResultCb",
                                            "(IILcom/rqg/alivesdk/FrameData;)V");
    bmDetectAlive = env->GetStaticMethodID(cbClazz, "detectAlive", "(Ljava/lang/String;I)I");
    faceQualityResultCb = env->GetStaticMethodID(cbClazz, "faceQualityResultCb",
                                                 "(ZFF)V");
    jclass frameDataClass = env->FindClass("com/rqg/alivesdk/FrameData");
    frameDataClass_g = (jclass) env->NewGlobalRef(frameDataClass);
    bool ret = ty_ap_init_anti_spoof(model_path, result_cb, frame_finish_cb) == 0;

    ty_ap_set_verify_timeout_ms(5000);

    return ret ? JNI_TRUE : JNI_FALSE;
}

jboolean
send_nv12_frame(JNIEnv *env, jclass, jobject _buf, jint rotation, jint width, jint height, jlong ts,
                jboolean eof) {
//    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();

    auto buf = env->GetDirectBufferAddress(_buf);

    auto *frame_buf = static_cast<uint8_t *>(malloc(width * height * 3));

    cv::Mat _yuv(height + height / 2, width, CV_8UC1, (uchar *) buf);

    cv::Mat bgrImg((rotation == 2 || rotation == 3) ? width : height,
                   (rotation == 2 || rotation == 3) ? height : width,
                   CV_8UC3, frame_buf);

    switch (rotation) {
        case 1: {
            cv::cvtColor(_yuv, bgrImg, cv::COLOR_YUV2BGR_NV12);
        }
            break;
        case 2: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 1);
        }
            break;
        case 3: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 0);
        }
            break;
        case 4:
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::flip(tmp, bgrImg, -1);
            break;
    }

    {
//        static int iiii = 0;
//        std::stringstream ss;
//        ss << "/storage/emulated/0/Android/data/com.rqg.tvmalivedemo/files/dump/frame_dump_" << iiii << ".jpg";
//        ALOGD("dump h:%d, w:%d, r:%d, hw:%d,%d,  t_hw:%d,%d,", bgrImg.rows, bgrImg.cols, rotation, height, width,
//              (rotation == 2 || rotation == 3) ? width : height,
//              (rotation == 2 || rotation == 3) ? height : width);
//        cv::Mat dump(bgrImg.rows, bgrImg.cols, CV_8UC3, frame_buf);
//        cv::imwrite(ss.str(), dump);
//        iiii++;
    }

    TY_AP_FRAME frame{};
    frame.frame_data = frame_buf;
    frame.width = bgrImg.cols;
    frame.height = bgrImg.rows;
    frame.timestamp_millis = ts;
    frame.eof = eof == JNI_TRUE ? 1 : 0;

//    ALOGV("send_nv12_frame %lld",frame.timestamp_millis);


    auto ret = ty_ap_send_frame(frame);

    if (ret != 0) {
        free(frame_buf);
    }

//    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
//    auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();
//    ALOGV("send_nv12_frame time cost %lld", millis);

    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}

jboolean send_yuv420_888_frame(JNIEnv *env, jclass, jobject _buf_y, jobject _buf_u, jobject _buf_v,
                               jint rotation, jint width, jint height, jlong ts, jboolean eof) {
//    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();

    auto bufY = env->GetDirectBufferAddress(_buf_y);
    auto bufU = env->GetDirectBufferAddress(_buf_u);
    auto bufV = env->GetDirectBufferAddress(_buf_v);


//    ALOGD("send_yuv420_888_frame %p, %p, %p", bufY, bufU, bufV);

    uint8_t *buf = static_cast<uint8_t *>(malloc(width * height * 3 / 2));

    //todo 只适配 yuv420_888 实际颜色格式是 n12
    memcpy(buf, bufY, width * height); //copy y
    memcpy(buf + width * height, bufU, width * height / 2);// copy uv

    auto *frame_buf = static_cast<uint8_t *>(malloc(width * height * 3));
    cv::Mat _yuv(height + height / 2, width, CV_8UC1, (uchar *) buf);

    cv::Mat bgrImg((rotation == 2 || rotation == 3) ? width : height,
                   (rotation == 2 || rotation == 3) ? height : width,
                   CV_8UC3, frame_buf);

    switch (rotation) {
        case 1: {
            cv::cvtColor(_yuv, bgrImg, cv::COLOR_YUV2BGR_NV12);
        }
            break;
        case 2: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 1);
        }
            break;
        case 3: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 0);
        }
            break;
        case 4:
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_NV12);
            cv::flip(tmp, bgrImg, -1);
            break;
    }

    free(buf);

//    {
//        static int iiii = 0;
//        std::stringstream ss;
//        ss << "/storage/emulated/0/Android/data/com.rqg.camerademo/files/dump/frame_dump_" << iiii << ".jpg";
//        ALOGD("dump h:%d, w:%d, r:%d, hw:%d,%d,  t_hw:%d,%d,", bgrImg.rows, bgrImg.cols, rotation, height, width,
//              (rotation == 2 || rotation == 3) ? width : height,
//              (rotation == 2 || rotation == 3) ? height : width);
//        cv::Mat dump(bgrImg.rows, bgrImg.cols, CV_8UC3, frame_buf);
//        cv::imwrite(ss.str(), dump);
//        iiii++;
//    }

    TY_AP_FRAME frame{};
    frame.frame_data = frame_buf;
    frame.width = bgrImg.cols;
    frame.height = bgrImg.rows;
    frame.timestamp_millis = ts;
    frame.eof = eof == JNI_TRUE ? 1 : 0;

//    ALOGV("send_nv12_frame %lld",frame.timestamp_millis);


    auto ret = ty_ap_send_frame(frame);

    if (ret != 0) {
        free(frame_buf);
    }

//    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
//    auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();
//    ALOGV("send_nv12_frame time cost %lld", millis);

    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}

jboolean
send_yv12_frame(JNIEnv *env, jclass, jobject _buf, jint rotation, jint width, jint height, jlong ts,
                jboolean eof) {
//    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();

    auto yv12_buf = env->GetDirectBufferAddress(_buf);

    auto *frame_buf = static_cast<uint8_t *>(malloc(width * height * 3));
    cv::Mat _yuv(height + height / 2, width, CV_8UC1, (uchar *) yv12_buf);

    cv::Mat bgrImg((rotation == 2 || rotation == 3) ? width : height,
                   (rotation == 2 || rotation == 3) ? height : width,
                   CV_8UC3, frame_buf);

    switch (rotation) {
        case 1: {
            cv::cvtColor(_yuv, bgrImg, cv::COLOR_YUV2BGR_YV12);
        }
            break;
        case 2: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 1);
        }
            break;
        case 3: {
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::transpose(tmp, tmp);
            cv::flip(tmp, bgrImg, 0);
        }
            break;
        case 4:
            cv::Mat tmp;
            cv::cvtColor(_yuv, tmp, cv::COLOR_YUV2BGR_YV12);
            cv::flip(tmp, bgrImg, -1);
            break;
    }

//    {
//        static int iiii = 0;
//        std::stringstream ss;
//        ss << "/storage/emulated/0/Android/data/com.rqg.camerademo/files/dump/frame_dump_" << iiii << ".jpg";
//        ALOGD("dump h:%d, w:%d, r:%d, hw:%d,%d,  t_hw:%d,%d,", bgrImg.rows, bgrImg.cols, rotation, height, width,
//              (rotation == 2 || rotation == 3) ? width : height,
//              (rotation == 2 || rotation == 3) ? height : width);
//        cv::Mat dump(bgrImg.rows, bgrImg.cols, CV_8UC3, frame_buf);
//        cv::imwrite(ss.str(), dump);
//        iiii++;
//    }

    TY_AP_FRAME frame{};
    frame.frame_data = frame_buf;
    frame.width = bgrImg.cols;
    frame.height = bgrImg.rows;
    frame.timestamp_millis = ts;
    frame.eof = eof == JNI_TRUE ? 1 : 0;

//    ALOGV("send_nv12_frame %lld",frame.timestamp_millis);


    auto ret = ty_ap_send_frame(frame);

    if (ret != 0) {
        free(frame_buf);
    }

//    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
//    auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();
//    ALOGV("send_nv12_frame time cost %lld", millis);

    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}


jboolean verify_action(JNIEnv *env, jclass, jint action) {
    return ty_ap_verify_action(action) == 0 ? JNI_TRUE : JNI_FALSE;
}

jstring get_alg_version(JNIEnv *env, jclass) {
    auto version = ty_ap_get_version();
    return env->NewStringUTF(version);
}

void discard_verify(JNIEnv *env, jclass) {
    ty_ap_discard_verify();
}

//void destroy_anti_spoof(JNIEnv *env, jclass) {
////    ALOGV("ap_destroy_anti_spoof ");
//    ty_ap_destroy_anti_spoof();
//}

void handle_detect_alive(const httplib::Request &req, httplib::Response &resp) {
    auto fImg = req.get_file_value("image");
    auto action_name = req.get_file_value("action_name");
    uint64_t verifyTimeout = 3000;
    float spoofRatio = 0.2f;
    bool reqCostTime = false;

    if (req.has_param("cost-time") && req.get_param_value("cost-time", 0) == "true") {
        reqCostTime = true;
    }

    auto timeoutStr = req.get_file_value("verify_timeout_ms");
    if (timeoutStr.content.length() > 0) {
        verifyTimeout = atoi(timeoutStr.content.c_str());
    }

    auto spoofRatioStr = req.get_file_value("spoof_ratio");
    if (spoofRatioStr.content.length() > 0) {
        spoofRatio = atof(spoofRatioStr.content.c_str());
    }
    ty_ap_set_verify_timeout_ms(verifyTimeout);
    ty_ap_set_spoof_ratio(spoofRatio);
//    ALOGD("verifyTimeout:%llu, spoofRatio:%f ", verifyTimeout, spoofRatio);

    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();
    int action = TY_AP_ACTION_BLINK;

    if (action_name.content == "blink") {
        action = TY_AP_ACTION_BLINK;
    } else if (action_name.content == "open_mouth") {
        action = TY_AP_ACTION_OPEN_MOUTH;
    } else if (action_name.content == "nod_head") {
        action = TY_AP_ACTION_NOD_HEAD;
    } else if (action_name.content == "shake_head") {
        action = TY_AP_ACTION_SHAKE_HEAD;
    }

    auto tmpFilePath = benchamrkCacheDir + "/test_video.mp4";

//    ALOGV("tmpFilePath: %s, %p", tmpFilePath.data(), bmDetectAlive);
    auto f = fopen(tmpFilePath.data(), "wb");
    fwrite(fImg.content.data(), 1, fImg.content.length(), f);
    fflush(f);
    fclose(f);

    PREPARE_JNI_ENV
    auto vPath = cbEnv->NewStringUTF(tmpFilePath.data());
    auto ret = cbEnv->CallStaticIntMethod(cbClazz, bmDetectAlive, vPath, action);

    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
    auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();

    remove(tmpFilePath.data());

    rapidjson::StringBuffer s;
    rapidjson::Writer<rapidjson::StringBuffer> writer(s);
    writer.StartArray();
    writer.StartObject();
    writer.Key("alive");
    ALOGD("ret %d", ret);
    switch (ret) {
        case TY_AP_RESULT_ALIVE:
            writer.String("alive");
            break;
        case TY_AP_RESULT_FAKE:
            writer.String("fake");
            break;
        case TY_AP_RESULT_MULTI_FACE:
            writer.String("multi_face");
            break;
        case TY_AP_RESULT_NO_FACE:
            writer.String("no_face");
            break;
        case TY_AP_RESULT_SPOOF_FAKE:
            writer.String("spoof_fake");
            break;
        case TY_AP_RESULT_CONTINUOUS_FAKE:
            writer.String("continuous_fake");
            break;
        case TY_AP_RESULT_ACTION_FAKE:
            writer.String("action_fake");
            break;
        default:
            writer.String("fake_default");
            break;
    }

    writer.EndObject();

    if (reqCostTime) {
        writer.StartObject();
        writer.Key("cost-time");
        writer.Int(millis);
        writer.EndObject();
    }

    writer.EndArray();

//    printf("%s\n", s.GetString());
    resp.set_content(s.GetString(), "text/plain");
}


void start_benchmark_server(JNIEnv *env, jclass, jint port, jstring _cacheDir) {
    svr.Post("/tuya-ai/face/cooperation_alive", handle_detect_alive);

    auto cacheDir = env->GetStringUTFChars(_cacheDir, nullptr);
    benchamrkCacheDir = cacheDir;
    env->ReleaseStringUTFChars(_cacheDir, cacheDir);

    ALOGD("start benchmark server on port %d", port);
    svr.listen("0.0.0.0", port);
    ALOGD("finish serve");
}

void set_verify_timeout_ms(JNIEnv *env, jclass, jlong _timeout_ms) {
    ty_ap_set_verify_timeout_ms(_timeout_ms);
}

void destroy_anti_spoof(JNIEnv *env, jclass) {
    //demo 使用了 tread 的方式获取 rect， 这里也仅是演示需要
//    {
//        run_get_face_rect = false;
//        tThread->join();
//    }

    ty_ap_destroy_anti_spoof();

}

static const char *classPathName = "com/rqg/alivesdk/BridgeNative";
static JNINativeMethod methods[] = {
        {"init_anti_spoof",        "(Ljava/lang/String;)Z",                                                   (void *) init_anti_spoof},
        {"send_nv12_frame",        "(Ljava/nio/ByteBuffer;IIIJZ)Z",                                           (void *) send_nv12_frame},
        {"send_yuv420_888_frame",  "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;IIIJZ)Z", (void *) send_yuv420_888_frame},
        {"send_yv12_frame",        "(Ljava/nio/ByteBuffer;IIIJZ)Z",                                           (void *) send_yv12_frame},
        {"verify_action",          "(I)Z",                                                                    (void *) verify_action},
        {"get_alg_version",        "()Ljava/lang/String;",                                                    (void *) get_alg_version},
        {"start_benchmark_server", "(ILjava/lang/String;)V",                                                  (void *) start_benchmark_server},
        {"set_verify_timeout_ms",  "(J)V",                                                                    (void *) set_verify_timeout_ms},
        {"destroy_anti_spoof",     "()V",                                                                     (void *) destroy_anti_spoof},
        {"discard_verify",         "()V",                                                                     (void *) discard_verify},
        {"get_face_rect_info",     "(Ljava/nio/ByteBuffer;IIIJZ)Z",                                           (void *) get_face_rect_info},
        {"get_face_quality",       "(Ljava/nio/ByteBuffer;IIIJZ)Z",                                           (void *) get_face_quality},
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, classPathName,
                               methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
// ----------------------------------------------------------------------------
/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv *env = NULL;
    ALOGI("JNI_OnLoad");
    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;
    if (registerNatives(env) != JNI_TRUE) {
        ALOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

    bail:
    return result;
}