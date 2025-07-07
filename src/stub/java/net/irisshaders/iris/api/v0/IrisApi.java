package net.irisshaders.iris.api.v0;

public interface IrisApi {
    static IrisApi getInstance() {
		throw new RuntimeException();
	}

    boolean isShaderPackInUse();
}
