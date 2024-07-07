package com.sdt.diagnose.common;

import java.util.List;

public interface IProtocolArray<T> {
    List<T> getArray();
    String getValue(T t, String[] paramsArr);

}
