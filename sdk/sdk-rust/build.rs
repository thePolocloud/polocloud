use std::{env, fs};
use std::path::{Path, PathBuf};

fn main() {
    let path = Path::new("../../proto/src/main/proto/polocloud/v1/proto/");

    let paths = fs::read_dir(path).unwrap();

    let path_strs: Vec<String> = paths
        .filter_map(|entry| entry.ok())
        .map(|entry| entry.path())
        .filter(|path| path.extension().is_some_and(|ext| ext == "proto"))
        .map(|path| path.to_string_lossy().to_string())
        .collect();

    build_protos(&path_strs);
}

fn build_protos(paths: &[String]) {
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    tonic_prost_build::configure()
        .file_descriptor_set_path(out_dir.join("./polocloud-proto.bin"))
        .type_attribute("ServiceState", "#[derive(serde::Deserialize)]")
        .type_attribute("GroupType", "#[derive(serde::Deserialize)]")
        .compile_protos(
            paths,
            &["../../proto/src/main/proto".to_string()],
        )
        .unwrap();
}
