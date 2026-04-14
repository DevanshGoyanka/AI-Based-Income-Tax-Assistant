import { useState } from 'react';
import Card from '../components/ui/Card';
import Tabs from '../components/ui/Tabs';
import Button from '../components/ui/Button';

export default function Reports() {
  const [activeTab, setActiveTab] = useState('filing');

  const tabs = [
    { id: 'filing', label: 'Filing Analytics' },
    { id: 'revenue', label: 'Revenue Trends' },
    { id: 'client', label: 'Client Growth' },
    { id: 'performance', label: 'Team Performance' },
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <div className="flex gap-2">
          <select className="px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
            <option>Last 30 Days</option>
            <option>Last Quarter</option>
            <option>Last Year</option>
            <option>Custom Range</option>
          </select>
        </div>
        <Button variant="secondary">📊 Export PDF</Button>
      </div>

      <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

      {activeTab === 'filing' && (
        <Card className="mt-4">
          <div className="text-[14px] font-semibold mb-4">Filing Analytics — Last 30 Days</div>
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Total Filings</div>
              <div className="font-serif text-[28px] font-semibold">487</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Success Rate</div>
              <div className="font-serif text-[28px] font-semibold text-success">94.2%</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Avg. Processing Time</div>
              <div className="font-serif text-[28px] font-semibold">2.4h</div>
            </div>
          </div>
          <div className="h-64 flex items-end justify-around gap-2 border-b border-border pb-2">
            {[65, 82, 78, 91, 88, 95, 102, 98, 87, 93, 89, 96].map((val, i) => (
              <div key={i} className="flex-1 bg-accent-blue rounded-t" style={{ height: `${(val / 102) * 100}%` }} />
            ))}
          </div>
          <div className="flex justify-around mt-2 text-[11px] text-text-muted">
            {['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'].map(m => (
              <span key={m}>{m}</span>
            ))}
          </div>
        </Card>
      )}

      {activeTab === 'revenue' && (
        <Card className="mt-4">
          <div className="text-[14px] font-semibold mb-4">Revenue Trends</div>
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Total Revenue</div>
              <div className="font-serif text-[28px] font-semibold text-success">₹12.6L</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Growth Rate</div>
              <div className="font-serif text-[28px] font-semibold text-accent-blue">+18%</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Avg. Invoice Value</div>
              <div className="font-serif text-[28px] font-semibold">₹2,587</div>
            </div>
          </div>
          <div className="h-64 flex items-end justify-around gap-2 border-b border-border pb-2">
            {[45, 52, 48, 61, 58, 67, 72, 68, 74, 79, 82, 88].map((val, i) => (
              <div key={i} className="flex-1 bg-success rounded-t" style={{ height: `${(val / 88) * 100}%` }} />
            ))}
          </div>
          <div className="flex justify-around mt-2 text-[11px] text-text-muted">
            {['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'].map(m => (
              <span key={m}>{m}</span>
            ))}
          </div>
        </Card>
      )}

      {activeTab === 'client' && (
        <Card className="mt-4">
          <div className="text-[14px] font-semibold mb-4">Client Growth</div>
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Total Clients</div>
              <div className="font-serif text-[28px] font-semibold">1,247</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">New This Month</div>
              <div className="font-serif text-[28px] font-semibold text-accent-teal">+42</div>
            </div>
            <div className="p-4 bg-bg rounded-lg">
              <div className="text-[11px] text-text-muted uppercase mb-1">Retention Rate</div>
              <div className="font-serif text-[28px] font-semibold text-success">96.8%</div>
            </div>
          </div>
          <div className="h-64 flex items-end justify-around gap-2 border-b border-border pb-2">
            {[820, 845, 872, 901, 935, 968, 1002, 1041, 1078, 1119, 1165, 1247].map((val, i) => (
              <div key={i} className="flex-1 bg-accent-teal rounded-t" style={{ height: `${(val / 1247) * 100}%` }} />
            ))}
          </div>
          <div className="flex justify-around mt-2 text-[11px] text-text-muted">
            {['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'].map(m => (
              <span key={m}>{m}</span>
            ))}
          </div>
        </Card>
      )}

      {activeTab === 'performance' && (
        <Card className="mt-4">
          <div className="text-[14px] font-semibold mb-4">Team Performance</div>
          <div className="space-y-3">
            {[
              { name: 'Advocate Sharma', filings: 142, revenue: 356000, satisfaction: 98 },
              { name: 'Advocate Patel', filings: 128, revenue: 312000, satisfaction: 96 },
              { name: 'Advocate Kumar', filings: 117, revenue: 289000, satisfaction: 94 },
            ].map(member => (
              <div key={member.name} className="p-4 border border-border rounded-lg">
                <div className="flex items-center justify-between mb-3">
                  <div className="text-[14px] font-semibold">{member.name}</div>
                  <div className="text-[12px] text-text-muted">{member.satisfaction}% satisfaction</div>
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <div className="text-[11px] text-text-muted uppercase mb-1">Filings</div>
                    <div className="text-[18px] font-serif font-semibold">{member.filings}</div>
                  </div>
                  <div>
                    <div className="text-[11px] text-text-muted uppercase mb-1">Revenue</div>
                    <div className="text-[18px] font-serif font-semibold text-success">₹{(member.revenue / 1000).toFixed(0)}K</div>
                  </div>
                  <div>
                    <div className="text-[11px] text-text-muted uppercase mb-1">Avg. Time</div>
                    <div className="text-[18px] font-serif font-semibold">2.1h</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
