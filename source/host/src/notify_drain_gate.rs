pub const NOTIFY_SAFETY_POLL_MS:u64=250;
pub const NOTIFY_DRAIN_FLOOR_MS:u64=500;

#[derive(Default)]
pub struct NotifyDrainGate{pending:usize,last_change_ms:u64}
impl NotifyDrainGate{
    pub fn begin(&mut self,now_ms:u64){self.pending+=1;self.last_change_ms=now_ms;}
    pub fn end(&mut self,now_ms:u64){self.pending=self.pending.saturating_sub(1);self.last_change_ms=now_ms;}
    pub fn is_drained(&self,now_ms:u64)->bool{self.pending==0&&now_ms.saturating_sub(self.last_change_ms)>=NOTIFY_DRAIN_FLOOR_MS}
}
