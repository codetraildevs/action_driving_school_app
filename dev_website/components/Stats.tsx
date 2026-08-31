import { stats } from "@/data/stats"

const Stats: React.FC = () => {
    return (
        <section id="stats" className="py-16 md:py-24 px-6">
            <div className="max-w-7xl mx-auto grid sm:grid-cols-3 gap-12 md:gap-8">
                {stats.map(stat => (
                    <div key={stat.title} className="text-center sm:text-left max-w-md sm:max-w-full mx-auto">
                        <h3 className="mb-4 flex items-center gap-3 text-3xl md:text-4xl font-bold justify-center sm:justify-start">
                            {stat.icon}
                            {stat.title}
                        </h3>
                        <p className="text-foreground-accent leading-relaxed">{stat.description}</p>
                    </div>
                ))}
            </div>
        </section>
    )
}

export default Stats
