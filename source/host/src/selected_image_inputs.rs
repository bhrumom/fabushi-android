use std::path::{Path,PathBuf};
#[derive(Clone,Debug,PartialEq,Eq)]
pub struct SelectedImageInput{pub path:PathBuf,pub media_type:String}
pub fn load_selected_image_inputs(paths:&[PathBuf])->Vec<SelectedImageInput>{
    paths.iter().filter(|path|path.is_file()).filter_map(|path|{
        let ext=path.extension()?.to_str()?.to_ascii_lowercase();
        let media=match ext.as_str(){"png"=>"image/png","jpg"|"jpeg"=>"image/jpeg","webp"=>"image/webp","gif"=>"image/gif",_=>return None};
        Some(SelectedImageInput{path:path.clone(),media_type:media.into()})
    }).collect()
}
pub fn is_supported_image(path:&Path)->bool{["png","jpg","jpeg","webp","gif"].contains(&path.extension().and_then(|v|v.to_str()).unwrap_or("").to_ascii_lowercase().as_str())}
