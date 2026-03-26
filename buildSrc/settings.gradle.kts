// 显式固定 buildSrc 的逻辑工程名，避免不同检出路径影响
// configuration cache 和 type-safe accessors 的稳定性。
rootProject.name = "buildSrc"
