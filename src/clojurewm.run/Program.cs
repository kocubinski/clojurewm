using System.Reflection;
using clojure.lang.Hosting;

namespace clojurewm.run
{
    class Program
    {
        static public void Main(string[] args)
        {
            Assembly.Load("clojurewm");
            Clojure.Require("clojurewm.init").main();
        }
    }
}
