import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';

export default function Tasks() {
  const columns = [
    { id: 'pending', title: 'Pending', tasks: [
      { id: 1, client: 'Rajesh Kumar', task: 'Upload Form 16', due: 'Apr 12', priority: 'high' },
      { id: 2, client: 'Priya Sharma', task: 'Reconcile 26AS', due: 'Apr 13', priority: 'normal' },
    ]},
    { id: 'progress', title: 'In Progress', tasks: [
      { id: 3, client: 'Amit Patel', task: 'Compute ITR-2', due: 'Apr 11', priority: 'high' },
    ]},
    { id: 'done', title: 'Done', tasks: [
      { id: 4, client: 'Neha Gupta', task: 'File ITR-1', due: 'Apr 10', priority: 'normal' },
    ]},
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <div className="flex gap-2">
          <select className="px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
            <option>All Tasks</option>
            <option>My Tasks</option>
            <option>High Priority</option>
          </select>
        </div>
        <Button variant="primary">+ Add Task</Button>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {columns.map(col => (
          <Card key={col.id}>
            <div className="flex items-center justify-between mb-4">
              <div className="text-[14px] font-semibold">{col.title}</div>
              <Badge label={col.tasks.length.toString()} variant="neutral" />
            </div>
            <div className="space-y-3">
              {col.tasks.map(task => (
                <div key={task.id} className="p-3 bg-bg border border-border rounded-lg cursor-move hover:shadow-sm transition-shadow">
                  <div className="flex items-start justify-between mb-2">
                    <div className="text-[13px] font-medium">{task.client}</div>
                    <div className={`w-2 h-2 rounded-full ${task.priority === 'high' ? 'bg-danger' : 'bg-accent-blue'}`} />
                  </div>
                  <div className="text-[12px] text-text-muted mb-2">{task.task}</div>
                  <div className="text-[11px] text-text-muted">Due: {task.due}</div>
                </div>
              ))}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
