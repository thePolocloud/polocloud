use crate::proto::service::Service;

macro_rules! impl_event {
    ($name:ident { $($field:ident: $t:ty),*$(,)? }) => {
        #[derive(serde::Deserialize, Debug)]
        pub struct $name {
            $(pub $field: $t),*
        }

        impl Event for $name {
            fn event_name() -> &'static str {
                stringify!($name)
            }
        }
    };
}

pub trait Event: Send + Sync {
    fn event_name() -> &'static str;
}

impl_event! {ServiceOnlineEvent { service: Service }}
impl_event! {ServiceShutdownEvent { service: Service }}
